package com.amannmalik.mcp.transport;

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
import com.amannmalik.mcp.security.OriginValidator;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
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

/** Jetty-based HTTP transport with basic SSE support. */
public final class StreamableHttpTransport implements Transport {
    private final Server server;
    private final int port;
    private final OriginValidator originValidator;
    private static final String PROTOCOL_HEADER = "MCP-Protocol-Version";
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final Set<SseClient> sseClients = ConcurrentHashMap.newKeySet();
    private final AtomicReference<String> sessionId = new AtomicReference<>();
    private final ConcurrentHashMap<String, BlockingQueue<JsonObject>> responseQueues = new ConcurrentHashMap<>();

    public StreamableHttpTransport(int port, OriginValidator validator) throws Exception {
        server = new Server(new InetSocketAddress("127.0.0.1", port));
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.addServlet(new ServletHolder(new McpServlet()), "/");
        server.setHandler(ctx);
        server.start();
        this.port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
        this.originValidator = validator;
    }

    public StreamableHttpTransport(int port) throws Exception {
        this(port, new OriginValidator(Set.of("http://localhost", "http://127.0.0.1")));
    }

    public StreamableHttpTransport() throws Exception {
        this(0);
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
        sseClients.forEach(c -> c.send(message));
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
            // Close all SSE clients
            sseClients.forEach(client -> {
                try {
                    client.close();
                } catch (Exception e) {
                    // Log but continue cleanup
                }
            });
            sseClients.clear();
            
            // Clear response queues and interrupt any waiting threads
            responseQueues.values().forEach(queue -> {
                // Add a poison pill to unblock waiting threads
                queue.offer(Json.createObjectBuilder()
                    .add("error", Json.createObjectBuilder()
                        .add("code", -32603)
                        .add("message", "Transport closed"))
                    .build());
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
                resp.setHeader("Mcp-Session-Id", session);
            } else if (session == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            } else if (!session.equals(header)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            } else if (version == null || !version.equals(ProtocolLifecycle.SUPPORTED_VERSION)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            BlockingQueue<JsonObject> q = new LinkedBlockingQueue<>(1);
            if (obj.containsKey("id")) {
                responseQueues.put(obj.get("id").toString(), q);
            }

            try {
                incoming.put(obj);
                JsonObject response = q.poll(30, java.util.concurrent.TimeUnit.SECONDS);
                if (response == null) {
                    // Timeout waiting for response
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
                if (obj.containsKey("id")) {
                    responseQueues.remove(obj.get("id").toString());
                }
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
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
            if (!session.equals(header)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (version == null || !version.equals(ProtocolLifecycle.SUPPORTED_VERSION)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/event-stream;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-cache");
            resp.flushBuffer();
            AsyncContext ac = req.startAsync();
            ac.setTimeout(60000); // 60 second timeout to prevent resource leaks
            SseClient client = new SseClient(ac);
            sseClients.add(client);
            ac.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    sseClients.remove(client);
                    try {
                        client.close();
                    } catch (Exception e) {
                        // Log but continue
                    }
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    sseClients.remove(client);
                    try {
                        client.close();
                    } catch (Exception e) {
                        // Log but continue
                    }
                }

                @Override
                public void onError(AsyncEvent event) {
                    sseClients.remove(client);
                    try {
                        client.close();
                    } catch (Exception e) {
                        // Log but continue
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
            if (session == null || !session.equals(header)) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (version == null || !version.equals(ProtocolLifecycle.SUPPORTED_VERSION)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            sessionId.set(null);
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
        }

        void send(JsonObject msg) {
            if (closed) return;
            try {
                out.write("data: " + msg.toString() + "\n\n");
                out.flush();
            } catch (Exception e) {
                // Connection broken, mark as closed
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
                // Already closed or invalid state
            }
        }
    }
}
