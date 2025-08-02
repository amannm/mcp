package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.AuthorizationException;
import com.amannmalik.mcp.auth.AuthorizationManager;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.lifecycle.Protocol;
import com.amannmalik.mcp.security.OriginValidator;
import com.amannmalik.mcp.util.CloseUtil;
import com.amannmalik.mcp.wire.RequestMethod;
import com.amannmalik.mcp.transport.ResourceMetadata;
import com.amannmalik.mcp.transport.ResourceMetadataCodec;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParsingException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.EOFException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class StreamableHttpTransport implements Transport {
    private final Server server;
    private final int port;
    private final OriginValidator originValidator;
    private final AuthorizationManager authManager;
    private final String resourceMetadataUrl;
    private final String canonicalResource;
    private final java.util.List<String> authorizationServers;


    private static final String PROTOCOL_HEADER = TransportHeaders.PROTOCOL_VERSION;
    // Default to the previous protocol revision when the version header is
    // absent, as recommended for backwards compatibility.
    private static final String COMPATIBILITY_VERSION =
            Protocol.PREVIOUS_VERSION;
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private volatile boolean closed;
    private final Set<SseClient> generalClients = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, SseClient> requestStreams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SseClient> clientsByPrefix = new ConcurrentHashMap<>();
    private final AtomicReference<SseClient> lastGeneral = new AtomicReference<>();
    private final SessionManager sessions = new SessionManager(COMPATIBILITY_VERSION);
    private final ConcurrentHashMap<String, BlockingQueue<JsonObject>> responseQueues = new ConcurrentHashMap<>();

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
        ctx.addServlet(new ServletHolder(new McpServlet()), "/");
        ctx.addServlet(new ServletHolder(new MetadataServlet()), "/.well-known/oauth-protected-resource");
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
        try {
            terminateSession(false);
            responseQueues.forEach((id, queue) -> {
                RequestId reqId = RequestId.parse(id);
                JsonRpcError err = JsonRpcError.of(
                        reqId,
                        JsonRpcErrorCode.INTERNAL_ERROR,
                        "Transport closed");
                queue.offer(JsonRpcCodec.toJsonObject(err));
            });
            responseQueues.clear();

            server.stop();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void removeRequestStream(String key, SseClient client) {
        requestStreams.remove(key);
        clientsByPrefix.remove(client.prefix);
        CloseUtil.closeQuietly(client);
    }

    private AsyncListener requestStreamListener(String key, SseClient client) {
        return new AsyncListener() {
            @Override public void onComplete(AsyncEvent event) { removeRequestStream(key, client); }
            @Override public void onTimeout(AsyncEvent event) { removeRequestStream(key, client); }
            @Override public void onError(AsyncEvent event) { removeRequestStream(key, client); }
            @Override public void onStartAsync(AsyncEvent event) { }
        };
    }

    private void removeGeneralStream(SseClient client) {
        generalClients.remove(client);
        lastGeneral.set(client);
        CloseUtil.closeQuietly(client);
    }

    private AsyncListener generalStreamListener(SseClient client) {
        return new AsyncListener() {
            @Override public void onComplete(AsyncEvent event) { removeGeneralStream(client); }
            @Override public void onTimeout(AsyncEvent event) { removeGeneralStream(client); }
            @Override public void onError(AsyncEvent event) { removeGeneralStream(client); }
            @Override public void onStartAsync(AsyncEvent event) { }
        };
    }

    private Principal authorize(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (authManager == null) return null;
        try {
            return authManager.authorize(req.getHeader("Authorization"));
        } catch (AuthorizationException e) {
            unauthorized(resp);
            return null;
        }
    }

    private boolean verifyOrigin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!originValidator.isValid(req.getHeader("Origin"))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    private boolean validateAccept(HttpServletRequest req, HttpServletResponse resp, boolean post) throws IOException {
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

    private boolean validateSession(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    Principal principal,
                                    boolean initializing) throws IOException {
        return sessions.validate(req, resp, principal, initializing);
    }

    private void terminateSession(boolean recordId) {
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

    private class McpServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Principal principal = authorize(req, resp);
            if (principal == null && authManager != null) return;
            if (!verifyOrigin(req, resp)) return;
            if (!validateAccept(req, resp, true)) return;

            JsonObject obj;
            try (JsonReader reader = Json.createReader(req.getInputStream())) {
                obj = reader.readObject();
            } catch (JsonParsingException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            boolean initializing = RequestMethod.INITIALIZE.method()
                    .equals(obj.getString("method", null));

            if (!validateSession(req, resp, principal, initializing)) return;

            boolean hasMethod = obj.containsKey("method");
            boolean hasId = obj.containsKey("id");
            boolean isRequest = hasMethod && hasId;
            boolean isNotification = hasMethod && !hasId;
            boolean isResponse = !hasMethod && (obj.containsKey("result") || obj.containsKey("error"));

            if (isNotification || isResponse) {
                try {
                    incoming.put(obj);
                    resp.setStatus(HttpServletResponse.SC_ACCEPTED);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
                return;
            }

            if (!isRequest) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            if (initializing) {
                BlockingQueue<JsonObject> q = new LinkedBlockingQueue<>(1);
                responseQueues.put(obj.get("id").toString(), q);
                try {
                    incoming.put(obj);
                    JsonObject response = q.poll(30, TimeUnit.SECONDS);
                    if (response == null) {
                        resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                        return;
                    }
                    if (response.containsKey("result")) {
                        JsonObject result = response.getJsonObject("result");
                        if (result.containsKey("protocolVersion")) {
                            sessions.protocolVersion(result.getString("protocolVersion"));
                        }
                    }
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    resp.setHeader(PROTOCOL_HEADER, sessions.protocolVersion());
                    resp.getWriter().write(response.toString());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                } finally {
                    responseQueues.remove(obj.get("id").toString());
                }
            } else {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("text/event-stream;charset=UTF-8");
                resp.setHeader("Cache-Control", "no-cache");
                resp.setHeader(PROTOCOL_HEADER, sessions.protocolVersion());
                resp.flushBuffer();
                AsyncContext ac = req.startAsync();
                ac.setTimeout(0);
                SseClient client = new SseClient(ac);
                String key = obj.get("id").toString();
                requestStreams.put(key, client);
                clientsByPrefix.put(client.prefix, client);
                ac.addListener(requestStreamListener(key, client));
                try {
                    incoming.put(obj);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    removeRequestStream(key, client);
                    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Principal principal = authorize(req, resp);
            if (principal == null && authManager != null) return;
            if (!verifyOrigin(req, resp)) return;
            if (!validateAccept(req, resp, false)) return;
            if (!validateSession(req, resp, principal, false)) return;
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/event-stream;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-cache");
            resp.setHeader(PROTOCOL_HEADER, sessions.protocolVersion());
            resp.flushBuffer();
            AsyncContext ac = req.startAsync();
            ac.setTimeout(0);

            String lastEvent = req.getHeader("Last-Event-ID");
            SseClient found = null;
            long lastId = 0;
            if (lastEvent != null) {
                int idx = lastEvent.lastIndexOf('-');
                if (idx > 0) {
                    String prefix = lastEvent.substring(0, idx);
                    try {
                        lastId = Long.parseLong(lastEvent.substring(idx + 1));
                    } catch (NumberFormatException ignore) {
                    }
                    found = clientsByPrefix.get(prefix);
                    if (found != null) {
                        found.attach(ac, lastId);
                    }
                }
            }
            SseClient client;
            if (found == null) {
                client = new SseClient(ac);
                clientsByPrefix.put(client.prefix, client);
            } else {
                client = found;
                lastGeneral.set(null);
            }
            generalClients.add(client);
            ac.addListener(generalStreamListener(client));
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Principal principal = authorize(req, resp);
            if (principal == null && authManager != null) return;
            if (!verifyOrigin(req, resp)) return;
            if (!validateSession(req, resp, principal, false)) return;
            terminateSession(true);
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private class MetadataServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            ResourceMetadata meta = new ResourceMetadata(canonicalResource, authorizationServers);
            JsonObject body = ResourceMetadataCodec.toJsonObject(meta);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(body.toString());
        }
    }

}
