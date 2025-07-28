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
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/** Jetty-based HTTP transport with basic SSE support. */
public final class HttpTransport implements Transport {
    private final Server server;
    private final int port;
    private final BlockingQueue<JsonObject> incoming = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonObject> outgoing = new LinkedBlockingQueue<>();
    private final Set<SseClient> sseClients = ConcurrentHashMap.newKeySet();

    public HttpTransport(int port) throws Exception {
        server = new Server(new InetSocketAddress("127.0.0.1", port));
        ServletContextHandler ctx = new ServletContextHandler();
        ctx.addServlet(new ServletHolder(new McpServlet()), "/");
        server.setHandler(ctx);
        server.start();
        this.port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    public HttpTransport() throws Exception {
        this(0);
    }

    public int port() {
        return port;
    }

    @Override
    public void send(JsonObject message) {
        outgoing.add(message);
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
            server.stop();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private class McpServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            try (JsonReader reader = Json.createReader(req.getInputStream())) {
                JsonObject obj = reader.readObject();
                incoming.put(obj);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            JsonObject response;
            try {
                response = outgoing.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                return;
            }
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(response.toString());
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/event-stream;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-cache");
            resp.flushBuffer();
            AsyncContext ac = req.startAsync();
            ac.setTimeout(0);
            SseClient client = new SseClient(ac);
            sseClients.add(client);
            ac.addListener(new AsyncListener() {
                @Override
                public void onComplete(AsyncEvent event) {
                    sseClients.remove(client);
                }

                @Override
                public void onTimeout(AsyncEvent event) {
                    sseClients.remove(client);
                }

                @Override
                public void onError(AsyncEvent event) {
                    sseClients.remove(client);
                }

                @Override
                public void onStartAsync(AsyncEvent event) {
                }
            });
        }
    }

    private static class SseClient {
        private final AsyncContext context;
        private final PrintWriter out;

        SseClient(AsyncContext context) throws IOException {
            this.context = context;
            this.out = context.getResponse().getWriter();
        }

        void send(JsonObject msg) {
            out.write("data: " + msg.toString() + "\n\n");
            out.flush();
        }
    }
}
