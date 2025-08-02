package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.*;
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
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public final class StreamableHttpTransport implements Transport {
    private final Server server;
    private final int port;
    private final OriginValidator originValidator;
    final AuthorizationManager authManager;
    private final String resourceMetadataUrl;
    final String canonicalResource;
    final java.util.List<String> authorizationServers;
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
                                   java.util.List<String> authorizationServers) throws Exception {
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
            this.authorizationServers = java.util.List.of();
        } else {
            this.authorizationServers = java.util.List.copyOf(authorizationServers);
        }
    }

    public StreamableHttpTransport(int port, OriginValidator validator, AuthorizationManager auth) throws Exception {
        this(port, validator, auth, null, java.util.List.of());
    }

    public int port() {
        return port;
    }

    @Override
    public void send(JsonObject message) {
        String id = message.containsKey("id") ? message.get("id").toString() : null;
        String method = message.getString("method", null);
        if (id != null) {
            if (sendToRequestStream(id, method, message)) return;
            if (sendToResponseQueue(id, message)) return;
            if (method == null) return;
        }
        if (!sendToActiveClient(message)) {
            sendToPending(message);
        }
    }

    private boolean sendToRequestStream(String id, String method, JsonObject message) {
        SseClient stream = requestStreams.get(id);
        if (stream == null) return false;
        stream.send(message);
        if (method == null) removeRequestStream(id, stream);
        return true;
    }

    private boolean sendToResponseQueue(String id, JsonObject message) {
        var q = responseQueues.remove(id);
        if (q == null) return false;
        q.add(message);
        return true;
    }

    private boolean sendToActiveClient(JsonObject message) {
        for (SseClient c : generalClients) {
            if (c.isActive()) {
                c.send(message);
                return true;
            }
        }
        return false;
    }

    private void sendToPending(JsonObject message) {
        SseClient pending = lastGeneral.get();
        if (pending != null) {
            pending.send(message);
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
            queue.offer(JsonRpcCodec.toJsonObject(err));
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

    Principal authorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (authManager == null) return null;
        try {
            return authManager.authorize(req.getHeader("Authorization"));
        } catch (AuthorizationException e) {
            unauthorized(resp);
            return null;
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
        String norm = accept.toLowerCase(java.util.Locale.ROOT);
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
        incoming.offer(Json.createObjectBuilder().add("_close", true).build());
    }

}
