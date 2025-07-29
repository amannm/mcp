package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
import com.amannmalik.mcp.security.OriginValidator;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
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

import java.util.concurrent.atomic.AtomicLong;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Set;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class StreamableHttpTransport implements Transport {
    private final Server server;
    private final int port;
    private final OriginValidator originValidator;
    private static final String PROTOCOL_HEADER = "MCP-Protocol-Version";
    // Default to the previous protocol revision when no version header is
    // present, as recommended for backwards compatibility.
    private static final String DEFAULT_VERSION = "2025-03-26";
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final Set<SseClient> sseClients = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, SseClient> requestStreams = new ConcurrentHashMap<>();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final AtomicReference<String> lastSessionId = new AtomicReference<>();
    private final AtomicReference<String> sessionOwner = new AtomicReference<>();
    private static final SecureRandom RANDOM = new SecureRandom();
    private volatile String protocolVersion;
    private final ConcurrentHashMap<String, BlockingQueue<JsonObject>> responseQueues = new ConcurrentHashMap<>();
    private final AtomicLong nextEventId = new AtomicLong(1);

    public StreamableHttpTransport(int port, OriginValidator validator) throws Exception {
        server = new Server(new InetSocketAddress("127.0.0.1", port));
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.addServlet(new ServletHolder(new McpServlet()), "/");
        server.setHandler(ctx);
        server.start();
        this.port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        this.originValidator = validator;
        // Until initialization negotiates a version, assume the prior revision
        // as the default when no MCP-Protocol-Version header is present.
        this.protocolVersion = DEFAULT_VERSION;
    }

    public StreamableHttpTransport(int port) throws Exception {
        this(port, new OriginValidator(Set.of("http://localhost", "http://127.0.0.1")));
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
                stream.send(message, nextEventId.getAndIncrement());
                if (method == null) {
                    stream.close();
                    requestStreams.remove(id);
                    sseClients.remove(stream);
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
        for (SseClient c : sseClients) {
            c.send(message, nextEventId.getAndIncrement());
            break;
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
            sseClients.forEach(client -> {
                try {
                    client.close();
                } catch (Exception ignore) {
                }
            });
            sseClients.clear();
            requestStreams.forEach((id, client) -> {
                try {
                    RequestId reqId;
                    if (id.startsWith("\"") && id.endsWith("\"") && id.length() > 1) {
                        reqId = new RequestId.StringId(id.substring(1, id.length() - 1));
                    } else {
                        try {
                            reqId = new RequestId.NumericId(Long.parseLong(id));
                        } catch (NumberFormatException e) {
                            reqId = new RequestId.StringId(id);
                        }
                    }
                    JsonRpcError err = new JsonRpcError(reqId,
                            new JsonRpcError.ErrorDetail(
                                    JsonRpcErrorCode.INTERNAL_ERROR.code(),
                                    "Transport closed",
                                    null));
                    client.send(JsonRpcCodec.toJsonObject(err), nextEventId.getAndIncrement());
                    client.close();
                } catch (Exception ignore) {
                }
            });
            requestStreams.clear();

            responseQueues.forEach((id, queue) -> {
                RequestId reqId;
                if (id.startsWith("\"") && id.endsWith("\"") && id.length() > 1) {
                    reqId = new RequestId.StringId(id.substring(1, id.length() - 1));
                } else {
                    try {
                        reqId = new RequestId.NumericId(Long.parseLong(id));
                    } catch (NumberFormatException e) {
                        reqId = new RequestId.StringId(id);
                    }
                }
                JsonRpcError err = new JsonRpcError(reqId,
                        new JsonRpcError.ErrorDetail(
                                JsonRpcErrorCode.INTERNAL_ERROR.code(),
                                "Transport closed",
                                null));
                queue.offer(JsonRpcCodec.toJsonObject(err));
            });
            responseQueues.clear();

            server.stop();
            sessionId.set(null);
            lastSessionId.set(null);
            sessionOwner.set(null);
            protocolVersion = ProtocolLifecycle.SUPPORTED_VERSION;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private class McpServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            if (!originValidator.isValid(req.getHeader("Origin"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String accept = req.getHeader("Accept");
            if (accept == null || !(accept.contains("application/json") && accept.contains("text/event-stream"))) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return;
            }

            String session = sessionId.get();
            String last = lastSessionId.get();
            String header = req.getHeader("Mcp-Session-Id");
            if (header != null && !isVisibleAscii(header)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            String version = req.getHeader(PROTOCOL_HEADER);
            JsonObject obj;
            try (JsonReader reader = Json.createReader(req.getInputStream())) {
                obj = reader.readObject();
            }
            boolean initializing = "initialize".equals(obj.getString("method", null));

            if (session == null && initializing) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                session = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                sessionId.set(session);
                sessionOwner.set(req.getRemoteAddr());
                lastSessionId.set(null);
                resp.setHeader("Mcp-Session-Id", session);
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
            } else if (version == null || !version.equals(protocolVersion)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
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
                sseClients.add(client);
                ac.addListener(new AsyncListener() {
                    @Override
                    public void onComplete(AsyncEvent event) {
                        requestStreams.remove(key);
                        sseClients.remove(client);
                        try {
                            client.close();
                        } catch (Exception e) {
                            System.err.println("SSE close failed: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onTimeout(AsyncEvent event) {
                        requestStreams.remove(key);
                        sseClients.remove(client);
                        try {
                            client.close();
                        } catch (Exception e) {
                            System.err.println("SSE close failed: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(AsyncEvent event) {
                        requestStreams.remove(key);
                        sseClients.remove(client);
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
                    sseClients.remove(client);
                    client.close();
                    resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                }
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!originValidator.isValid(req.getHeader("Origin"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String accept = req.getHeader("Accept");
            if (accept == null || !accept.contains("text/event-stream")) {
                resp.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
                return;
            }
            String session = sessionId.get();
            String last = lastSessionId.get();
            String header = req.getHeader("Mcp-Session-Id");
            String version = req.getHeader(PROTOCOL_HEADER);
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

            SseClient client = new SseClient(ac);
            sseClients.add(client);
            ac.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    sseClients.remove(client);
                    try {
                        client.close();
                    } catch (Exception e) {
                        System.err.println("SSE close failed: " + e.getMessage());
                    }
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    sseClients.remove(client);
                    try {
                        client.close();
                    } catch (Exception e) {
                        System.err.println("SSE close failed: " + e.getMessage());
                    }
                }

                @Override
                public void onError(AsyncEvent event) {
                    sseClients.remove(client);
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
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!originValidator.isValid(req.getHeader("Origin"))) {
                resp.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            String session = sessionId.get();
            String header = req.getHeader("Mcp-Session-Id");
            String version = req.getHeader(PROTOCOL_HEADER);
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
            if (version == null || !version.equals(protocolVersion)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            lastSessionId.set(session);
            sessionId.set(null);
            sessionOwner.set(null);
            protocolVersion = ProtocolLifecycle.SUPPORTED_VERSION;
            nextEventId.set(1);
            sseClients.forEach(SseClient::close);
            sseClients.clear();
            requestStreams.forEach((id, c) -> c.close());
            requestStreams.clear();
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private static class SseClient {
        private final AsyncContext context;
        private final PrintWriter out;
        private volatile boolean closed = false;

        SseClient(AsyncContext context) throws IOException {
            this.context = context;
            this.out = context.getResponse().getWriter();
            byte[] bytes = new byte[16];
            RANDOM.nextBytes(bytes);
        }

        void send(JsonObject msg, long id) {
            if (closed) return;
            try {
                out.write("id: " + id + "\n");
                out.write("data: " + msg.toString() + "\n\n");
                out.flush();
            } catch (Exception e) {
                System.err.println("SSE send failed: " + e.getMessage());
                closed = true;
            }
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
            }
        }
    }

    private static boolean isVisibleAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 0x21 || c > 0x7E) return false;
        }
        return true;
    }
}
