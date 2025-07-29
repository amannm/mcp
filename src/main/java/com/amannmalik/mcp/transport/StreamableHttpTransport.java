package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.security.OriginValidator;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.RequestId;
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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

public final class StreamableHttpTransport implements Transport {
    private final Server server;
    private final int port;
    private final OriginValidator originValidator;
    private static final String PROTOCOL_HEADER = "MCP-Protocol-Version";
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final Set<SseClient> sseClients = ConcurrentHashMap.newKeySet();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private volatile String protocolVersion;
    private final ConcurrentHashMap<String, BlockingQueue<JsonObject>> responseQueues = new ConcurrentHashMap<>();

    public StreamableHttpTransport(int port, OriginValidator validator) throws Exception {
        server = new Server(new InetSocketAddress("127.0.0.1", port));
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.addServlet(new ServletHolder(new McpServlet()), "/");
        server.setHandler(ctx);
        server.start();
        this.port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        this.originValidator = validator;
        this.protocolVersion = ProtocolLifecycle.SUPPORTED_VERSION;
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
        if (id != null) {
            var q = responseQueues.remove(id);
            if (q != null) {
                q.add(message);
                return;
            }
        }
        for (SseClient c : sseClients) {
            c.send(message);
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
            String header = req.getHeader("Mcp-Session-Id");
            String version = req.getHeader(PROTOCOL_HEADER);
            JsonObject obj;
            try (JsonReader reader = Json.createReader(req.getInputStream())) {
                obj = reader.readObject();
            }
            boolean initializing = "initialize".equals(obj.getString("method", null));

            if (session == null && initializing) {
                session = UUID.randomUUID().toString();
                sessionId.set(session);
                JsonObject params = obj.getJsonObject("params");
                protocolVersion = params == null ? ProtocolLifecycle.SUPPORTED_VERSION :
                        params.getString("protocolVersion", ProtocolLifecycle.SUPPORTED_VERSION);
                resp.setHeader("Mcp-Session-Id", session);
            } else if (session == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } else if (header == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } else if (!session.equals(header)) {
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

            BlockingQueue<JsonObject> q = new LinkedBlockingQueue<>(1);
            responseQueues.put(obj.get("id").toString(), q);

            try {
                incoming.put(obj);
                JsonObject response = q.poll(30, java.util.concurrent.TimeUnit.SECONDS);
                if (response == null) {
                    resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                    return;
                }
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().write(response.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } finally {
                responseQueues.remove(obj.get("id").toString());
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
            if (!session.equals(header)) {
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
            resp.flushBuffer();
            AsyncContext ac = req.startAsync();
            ac.setTimeout(60000); 

            String lastIdHeader = req.getHeader("Last-Event-ID");
            long lastId = -1;
            if (lastIdHeader != null) {
                try {
                    lastId = Long.parseLong(lastIdHeader);
                } catch (NumberFormatException ignore) {
                }
            }
            SseClient client = new SseClient(ac, lastId + 1);
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
            if (!session.equals(header)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (version == null || !version.equals(protocolVersion)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            sessionId.set(null);
            protocolVersion = ProtocolLifecycle.SUPPORTED_VERSION;
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private static class SseClient {
        private final AsyncContext context;
        private final PrintWriter out;
        private final java.util.concurrent.atomic.AtomicLong nextId;
        private volatile boolean closed = false;

        SseClient(AsyncContext context, long startId) throws IOException {
            this.context = context;
            this.out = context.getResponse().getWriter();
            this.nextId = new java.util.concurrent.atomic.AtomicLong(startId);
        }

        void send(JsonObject msg) {
            if (closed) return;
            long id = nextId.getAndIncrement();
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
}
