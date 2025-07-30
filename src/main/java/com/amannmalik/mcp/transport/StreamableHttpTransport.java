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
import com.amannmalik.mcp.util.Base64Util;
import com.amannmalik.mcp.wire.RequestMethod;
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
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
    private final Set<SseClient> generalClients = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, SseClient> requestStreams = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SseClient> clientsByPrefix = new ConcurrentHashMap<>();
    private final AtomicReference<SseClient> lastGeneral = new AtomicReference<>();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicReference<String> lastSessionId = new AtomicReference<>();
    private final AtomicReference<String> sessionOwner = new AtomicReference<>();
    private final AtomicReference<Principal> sessionPrincipal = new AtomicReference<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private volatile String protocolVersion;
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
            throw new IllegalArgumentException("authorizationServers required");
        }
        this.authorizationServers = java.util.List.copyOf(authorizationServers);
        // Until initialization negotiates a version, assume the prior revision
        // as the default when no MCP-Protocol-Version header is present.
        this.protocolVersion = COMPATIBILITY_VERSION;
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
            SseClient stream = requestStreams.get(id);
            if (stream != null) {
                stream.send(message);
                if (method == null) {
                    stream.close();
                    requestStreams.remove(id);
                    clientsByPrefix.remove(stream.prefix);
                }
                return;
            }
            var q = responseQueues.remove(id);
            if (q != null) {
                q.add(message);
                return;
            }
        }
        if (id != null && method == null) {
            return;
        }
        for (SseClient c : generalClients) {
            if (c.isActive()) {
                c.send(message);
                return;
            }
        }
        SseClient pending = lastGeneral.get();
        if (pending != null) {
            pending.send(message);
        }
    }

    @Override
    public JsonObject receive() throws IOException {
        try {
            return incoming.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            generalClients.forEach(client -> {
                try {
                    client.close();
                } catch (Exception ignore) {
                }
            });
            generalClients.clear();
            lastGeneral.set(null);
            requestStreams.forEach((id, client) -> {
                try {
                    RequestId reqId = RequestId.parse(id);
                    JsonRpcError err = JsonRpcError.of(
                            reqId,
                            JsonRpcErrorCode.INTERNAL_ERROR,
                            "Transport closed");
                    client.send(JsonRpcCodec.toJsonObject(err));
                    client.close();
                } catch (Exception ignore) {
                }
            });
            requestStreams.clear();
            clientsByPrefix.clear();

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
            sessionId.set(null);
            lastSessionId.set(null);
            sessionOwner.set(null);
            sessionPrincipal.set(null);
            // With the server shut down there is no negotiated protocol
            // version.  Reset to the default used when the version header is
            // absent.
            protocolVersion = COMPATIBILITY_VERSION;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private class McpServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            Principal principal = null;
            if (authManager != null) {
                try {
                    principal = authManager.authorize(req.getHeader("Authorization"));
                } catch (AuthorizationException e) {
                    unauthorized(resp);
                    return;
                }
            }
            if (!originValidator.isValid(req.getHeader("Origin"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String accept = req.getHeader("Accept");
            if (accept == null) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return;
            }
            String acceptNorm = accept.toLowerCase(java.util.Locale.ROOT);
            if (!(acceptNorm.contains("application/json") && acceptNorm.contains("text/event-stream"))) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return;
            }

            String session = sessionId.get();
            String last = lastSessionId.get();
            String header = req.getHeader(TransportHeaders.SESSION_ID);
            if (header != null && !isVisibleAscii(header)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String version = req.getHeader(PROTOCOL_HEADER);
            if (version != null && !isVisibleAscii(version)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            JsonObject obj;
            try (JsonReader reader = Json.createReader(req.getInputStream())) {
                obj = reader.readObject();
            } catch (JsonParsingException e) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            boolean initializing = RequestMethod.INITIALIZE.method()
                    .equals(obj.getString("method", null));

            if (session == null && initializing) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                session = Base64Util.encodeUrl(bytes);
                sessionId.set(session);
                sessionOwner.set(req.getRemoteAddr());
                sessionPrincipal.set(principal);
                lastSessionId.set(null);
                resp.setHeader(TransportHeaders.SESSION_ID, session);
            } else if (session == null) {
                if (header != null && header.equals(last)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
                return;
            } else if (header == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } else if (!session.equals(header) || !req.getRemoteAddr().equals(sessionOwner.get())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } else if (authManager != null && sessionPrincipal.get() != null && !sessionPrincipal.get().id().equals(principal.id())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            } else {
                if (!initializing && (version == null || !version.equals(protocolVersion))) {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }
            }

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
                            protocolVersion = result.getString("protocolVersion");
                        }
                    }
                    resp.setContentType("application/json");
                    resp.setCharacterEncoding("UTF-8");
                    resp.setHeader(PROTOCOL_HEADER, protocolVersion);
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
                resp.setHeader(PROTOCOL_HEADER, protocolVersion);
                resp.flushBuffer();
                AsyncContext ac = req.startAsync();
                ac.setTimeout(0);
                SseClient client = new SseClient(ac);
                String key = obj.get("id").toString();
                requestStreams.put(key, client);
                clientsByPrefix.put(client.prefix, client);
                ac.addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) {
                        requestStreams.remove(key);
                        clientsByPrefix.remove(client.prefix);
                        try {
                            client.close();
                        } catch (Exception e) {
                            System.err.println("SSE close failed: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) {
                        requestStreams.remove(key);
                        clientsByPrefix.remove(client.prefix);
                        try {
                            client.close();
                        } catch (Exception e) {
                            System.err.println("SSE close failed: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(AsyncEvent event) {
                        requestStreams.remove(key);
                        clientsByPrefix.remove(client.prefix);
                        try {
                            client.close();
                        } catch (Exception e) {
                            System.err.println("SSE close failed: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onStartAsync(AsyncEvent event) {
                    }
                });
                try {
                    incoming.put(obj);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    requestStreams.remove(key);
                    clientsByPrefix.remove(client.prefix);
                    client.close();
                    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Principal principal = null;
            if (authManager != null) {
                try {
                    principal = authManager.authorize(req.getHeader("Authorization"));
                } catch (AuthorizationException e) {
                    unauthorized(resp);
                    return;
                }
            }
            if (!originValidator.isValid(req.getHeader("Origin"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String accept = req.getHeader("Accept");
            if (accept == null) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return;
            }
            String acceptNorm = accept.toLowerCase(java.util.Locale.ROOT);
            if (!acceptNorm.contains("text/event-stream")) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return;
            }
            String session = sessionId.get();
            String last = lastSessionId.get();
            String header = req.getHeader(TransportHeaders.SESSION_ID);
            if (header != null && !isVisibleAscii(header)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String version = req.getHeader(PROTOCOL_HEADER);
            if (version != null && !isVisibleAscii(version)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (session == null) {
                if (header != null && header.equals(last)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else {
                    resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                }
                return;
            }
            if (header == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (!session.equals(header) || !req.getRemoteAddr().equals(sessionOwner.get())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (authManager != null && sessionPrincipal.get() != null && !sessionPrincipal.get().id().equals(principal.id())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            if (version == null || !version.equals(protocolVersion)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/event-stream;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-cache");
            resp.setHeader(PROTOCOL_HEADER, protocolVersion);
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
            final SseClient c = client;
            ac.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    generalClients.remove(c);
                    lastGeneral.set(c);
                    try {
                        c.close();
                    } catch (Exception e) {
                        System.err.println("SSE close failed: " + e.getMessage());
                    }
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    generalClients.remove(c);
                    lastGeneral.set(c);
                    try {
                        c.close();
                    } catch (Exception e) {
                        System.err.println("SSE close failed: " + e.getMessage());
                    }
                }

                @Override
                public void onError(AsyncEvent event) {
                    generalClients.remove(c);
                    lastGeneral.set(c);
                    try {
                        c.close();
                    } catch (Exception e) {
                        System.err.println("SSE close failed: " + e.getMessage());
                    }
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                }
            });
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            Principal principal = null;
            if (authManager != null) {
                try {
                    principal = authManager.authorize(req.getHeader("Authorization"));
                } catch (AuthorizationException e) {
                    unauthorized(resp);
                    return;
                }
            }
            if (!originValidator.isValid(req.getHeader("Origin"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String session = sessionId.get();
            String header = req.getHeader(TransportHeaders.SESSION_ID);
            if (header != null && !isVisibleAscii(header)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String version = req.getHeader(PROTOCOL_HEADER);
            if (version != null && !isVisibleAscii(version)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (session == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (header == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            if (!session.equals(header) || !req.getRemoteAddr().equals(sessionOwner.get())) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (authManager != null && sessionPrincipal.get() != null && !sessionPrincipal.get().id().equals(principal.id())) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (version != null && !version.equals(protocolVersion)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            lastSessionId.set(session);
            sessionId.set(null);
            sessionOwner.set(null);
            sessionPrincipal.set(null);
            // After the session ends the server no longer knows the negotiated
            // protocol version.  Reset to the backwards compatible default so
            // that a new session without a version header assumes the prior
            // revision as required by the specification.
            protocolVersion = COMPATIBILITY_VERSION;
            generalClients.forEach(SseClient::close);
            generalClients.clear();
            lastGeneral.set(null);
            requestStreams.forEach((id, c) -> c.close());
            requestStreams.clear();
            clientsByPrefix.clear();
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private class MetadataServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            var arr = jakarta.json.Json.createArrayBuilder();
            for (String s : authorizationServers) arr.add(s);
            var body = jakarta.json.Json.createObjectBuilder()
                    .add("resource", canonicalResource)
                    .add("authorization_servers", arr.build())
                    .build();
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(body.toString());
        }
    }

    private static class SseClient {
        private static final int HISTORY_LIMIT = 100;

        private AsyncContext context;
        private PrintWriter out;
        private final String prefix;
        private final Deque<SseEvent> history = new ArrayDeque<>();
        private final AtomicLong nextId = new AtomicLong(1);
        private volatile boolean closed = false;

        SseClient(AsyncContext context) throws IOException {
            byte[] bytes = new byte[8];
            RANDOM.nextBytes(bytes);
            this.prefix = Base64Util.encodeUrl(bytes);
            attach(context, 0);
        }

        void attach(AsyncContext ctx, long lastId) throws IOException {
            this.context = ctx;
            this.out = ctx.getResponse().getWriter();
            this.closed = false;
            sendHistory(lastId);
        }

        boolean isActive() {
            return !closed && context != null;
        }

        void send(JsonObject msg) {
            long id = nextId.getAndIncrement();
            history.addLast(new SseEvent(id, msg));
            while (history.size() > HISTORY_LIMIT) history.removeFirst();
            if (closed || context == null) return;
            try {
                out.write("id: " + prefix + '-' + id + "\n");
                out.write("data: " + msg.toString() + "\n\n");
                out.flush();
            } catch (Exception e) {
                System.err.println("SSE send failed: " + e.getMessage());
                closed = true;
            }
        }

        private void sendHistory(long lastId) throws IOException {
            if (context == null) return;
            for (SseEvent ev : history) {
                if (ev.id > lastId) {
                    out.write("id: " + prefix + '-' + ev.id + "\n");
                    out.write("data: " + ev.msg.toString() + "\n\n");
                }
            }
            out.flush();
        }

        void close() {
            if (closed) return;
            closed = true;
            try {
                if (context != null && !context.hasOriginalRequestAndResponse()) {
                    context.complete();
                }
            } catch (Exception e) {
                System.err.println("SSE close failed: " + e.getMessage());
            } finally {
                context = null;
                out = null;
            }
        }
    }

    private record SseEvent(long id, JsonObject msg) {
    }

    private static boolean isVisibleAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x21 || c > 0x7E) return false;
        }
        return true;
    }
}
