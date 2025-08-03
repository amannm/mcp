package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.lifecycle.Protocol;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;

public final class StreamableHttpTransport implements Transport {
    private final Server server;
    private final int port;
    private final OriginValidator originValidator;
    final AuthorizationManager authManager;
    private final String resourceMetadataUrl;
    final String canonicalResource;
    final List<String> authorizationServers;
    // Default to the previous protocol revision when the version header is
    // absent, as recommended for backwards compatibility.
    static final String COMPATIBILITY_VERSION =
            Protocol.PREVIOUS_VERSION;
    final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private volatile boolean closed;
    final SseClients clients = new SseClients();
    final SessionManager sessions = new SessionManager(COMPATIBILITY_VERSION);
    private final MessageRouter router;
    private final Queue<JsonObject> backlog = new ConcurrentLinkedQueue<>();

    private void unauthorized(HttpServletResponse resp) throws IOException {
        if (resourceMetadataUrl != null) {
            resp.setHeader("WWW-Authenticate", "Bearer resource_metadata=\"" + resourceMetadataUrl + "\"");
        }
        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    public StreamableHttpTransport(int port,
                                   OriginValidator validator,
                                   AuthorizationManager auth,
                                   String resourceMetadataUrl,
                                   List<String> authorizationServers) throws Exception {
        server = new Server(new InetSocketAddress("127.0.0.1", port));
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.addServlet(new ServletHolder(new McpServlet(this)), "/");
        ctx.addServlet(new ServletHolder(new MetadataServlet(this)), "/.well-known/oauth-protected-resource");
        server.setHandler(ctx);
        server.start();
        this.port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        this.originValidator = validator;
        this.authManager = auth;
        if (resourceMetadataUrl == null || resourceMetadataUrl.isBlank()) {
            this.resourceMetadataUrl = "http://127.0.0.1:" + this.port +
                    "/.well-known/oauth-protected-resource";
        } else {
            this.resourceMetadataUrl = resourceMetadataUrl;
        }
        this.canonicalResource = "http://127.0.0.1:" + this.port;
        this.authorizationServers = authorizationServers == null || authorizationServers.isEmpty()
                ? List.of()
                : List.copyOf(authorizationServers);
        this.router = new MessageRouter(clients.request, clients.responses, clients.general, clients.lastGeneral, clients::removeRequest);
    }

    public int port() {
        return port;
    }

    @Override
    public void send(JsonObject message) {
        if (router.route(message)) {
            flushBacklog();
        } else {
            backlog.add(message);
        }
    }

    void flushBacklog() {
        while (true) {
            JsonObject msg = backlog.peek();
            if (msg == null) return;
            if (router.route(msg)) {
                backlog.poll();
            } else {
                return;
            }
        }
    }

    @Override
    public JsonObject receive() throws IOException {
        try {
            JsonObject obj = incoming.take();
            if (closed && obj.containsKey("_close")) {
                throw new EOFException();
            }
            return obj;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        terminateSession(false);
        clients.failPending();
        try {
            server.stop();
            server.join();
            server.destroy();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    private static final Principal DEFAULT_PRINCIPAL = new Principal(
            McpConfiguration.current().security().auth().defaultPrincipal(), Set.of());

    Optional<Principal> authorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (authManager == null) return Optional.of(DEFAULT_PRINCIPAL);
        try {
            return Optional.of(authManager.authorize(req.getHeader("Authorization")));
        } catch (AuthorizationException e) {
            unauthorized(resp);
            return Optional.empty();
        }
    }

    boolean verifyOrigin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!originValidator.isValid(req.getHeader("Origin"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    boolean validateAccept(HttpServletRequest req, HttpServletResponse resp, boolean post) throws IOException {
        String accept = req.getHeader("Accept");
        if (accept == null) {
            resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
            return false;
        }
        String norm = accept.toLowerCase(Locale.ROOT);
        boolean ok = post
                ? norm.contains("application/json") && norm.contains("text/event-stream")
                : norm.contains("text/event-stream");
        if (!ok) resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        return ok;
    }

    boolean validateSession(HttpServletRequest req,
                            HttpServletResponse resp,
                            Principal principal,
                            boolean initializing) throws IOException {
        return sessions.validate(req, resp, principal, initializing);
    }

    void terminateSession(boolean recordId) {
        sessions.terminate(recordId);
        clients.clear();
        closed = true;
        if (!incoming.offer(Json.createObjectBuilder().add("_close", true).build())) {
            throw new IllegalStateException("incoming queue full");
        }
    }

}
