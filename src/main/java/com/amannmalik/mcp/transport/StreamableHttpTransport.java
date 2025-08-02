package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.Protocol;
import com.amannmalik.mcp.security.OriginValidator;
import com.amannmalik.mcp.util.CloseUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
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
import java.util.concurrent.atomic.AtomicReference;

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
    final Set<SseClient> generalClients = ConcurrentHashMap.newKeySet();
    final ConcurrentHashMap<String, SseClient> requestStreams = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, SseClient> clientsByPrefix = new ConcurrentHashMap<>();
    final AtomicReference<SseClient> lastGeneral = new AtomicReference<>();
    final SessionManager sessions = new SessionManager(COMPATIBILITY_VERSION);
    final ConcurrentHashMap<String, BlockingQueue<JsonObject>> responseQueues = new ConcurrentHashMap<>();
    private final MessageRouter router;

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
        int idx = this.resourceMetadataUrl.indexOf("/.well-known/oauth-protected-resource");
        if (idx >= 0) {
            this.canonicalResource = this.resourceMetadataUrl.substring(0, idx);
        } else {
            this.canonicalResource = "http://127.0.0.1:" + this.port;
        }
        if (authorizationServers == null || authorizationServers.isEmpty()) {
            this.authorizationServers = List.of();
        } else {
            this.authorizationServers = List.copyOf(authorizationServers);
        }
        this.router = new MessageRouter(requestStreams, responseQueues, generalClients, lastGeneral, this::removeRequestStream);
    }

    public StreamableHttpTransport(int port, OriginValidator validator, AuthorizationManager auth) throws Exception {
        this(port, validator, auth, null, List.of());
    }

    public int port() {
        return port;
    }

    @Override
    public void send(JsonObject message) {
        router.route(message);
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
        failPendingRequests();
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

    private void failPendingRequests() {
        responseQueues.forEach((id, queue) -> {
            RequestId reqId = RequestId.parse(id);
            JsonRpcError err = JsonRpcError.of(
                    reqId,
                    JsonRpcErrorCode.INTERNAL_ERROR,
                    "Transport closed");
            if (!queue.offer(JsonRpcCodec.toJsonObject(err))) {
                throw new IllegalStateException("queue full");
            }
        });
        responseQueues.clear();
    }

    void removeRequestStream(String key, SseClient client) {
        requestStreams.remove(key);
        clientsByPrefix.remove(client.prefix);
        CloseUtil.closeQuietly(client);
    }

    AsyncListener requestStreamListener(String key, SseClient client) {
        return new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                removeRequestStream(key, client);
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                removeRequestStream(key, client);
            }

            @Override
            public void onError(AsyncEvent event) {
                removeRequestStream(key, client);
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
            }
        };
    }

    private void removeGeneralStream(SseClient client) {
        generalClients.remove(client);
        lastGeneral.set(client);
        CloseUtil.closeQuietly(client);
    }

    AsyncListener generalStreamListener(SseClient client) {
        return new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                removeGeneralStream(client);
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                removeGeneralStream(client);
            }

            @Override
            public void onError(AsyncEvent event) {
                removeGeneralStream(client);
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
            }
        };
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
        if (post) {
            if (!(norm.contains("application/json") && norm.contains("text/event-stream"))) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return false;
            }
        } else {
            if (!norm.contains("text/event-stream")) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return false;
            }
        }
        return true;
    }

    boolean validateSession(HttpServletRequest req,
                            HttpServletResponse resp,
                            Principal principal,
                            boolean initializing) throws IOException {
        return sessions.validate(req, resp, principal, initializing);
    }

    void terminateSession(boolean recordId) {
        sessions.terminate(recordId);
        generalClients.forEach(SseClient::close);
        generalClients.clear();
        lastGeneral.set(null);
        requestStreams.forEach((id, c) -> c.close());
        requestStreams.clear();
        clientsByPrefix.clear();
        closed = true;
        if (!incoming.offer(Json.createObjectBuilder().add("_close", true).build())) {
            throw new IllegalStateException("incoming queue full");
        }
    }

}
