package com.amannmalik.mcp.transport;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/** Jetty-based HTTP server implementing the MCP Streamable HTTP transport. */
public final class StreamableHttpServer implements Transport {
    private final Server server;
    private final URI endpoint;
    private final AtomicReference<Session> sessionRef = new AtomicReference<>();

    public StreamableHttpServer(int port, String path) throws Exception {
        server = new Server(new InetSocketAddress("localhost", port));
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new TransportServlet()), path);
        server.setHandler(context);
        server.start();
        ServerConnector connector = (ServerConnector) server.getConnectors()[0];
        endpoint = URI.create("http://localhost:" + connector.getLocalPort() + path);
    }

    public URI endpoint() {
        return endpoint;
    }

    @Override
    public void send(JsonObject message) {
        Session session = sessionRef.get();
        if (session == null) throw new IllegalStateException("no session");
        session.outbound.offer(message);
    }

    @Override
    public JsonObject receive() throws IOException {
        Session session = sessionRef.get();
        if (session == null) throw new IOException("no session");
        try {
            return session.inbound.take();
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

    private static final class Session {
        final String id;
        final BlockingQueue<JsonObject> inbound = new ArrayBlockingQueue<>(16);
        final BlockingQueue<JsonObject> outbound = new ArrayBlockingQueue<>(16);
        volatile PrintWriter sse;

        Session(String id) {
            this.id = id;
        }
    }

    private final class TransportServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkOrigin(req, resp)) return;
            try (InputStream in = req.getInputStream(); JsonReader r = Json.createReader(in)) {
                JsonObject obj = r.readObject();
                Session session = getSession(req, resp, true);
                if (session == null) return;
                session.inbound.offer(obj);
                JsonObject out = take(session.outbound);
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("application/json");
                resp.getWriter().write(out.toString());
            }
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkOrigin(req, resp)) return;
            Session session = getSession(req, resp, false);
            if (session == null) return;
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/event-stream");
            resp.setHeader("Cache-Control", "no-cache");
            resp.flushBuffer();
            PrintWriter writer = resp.getWriter();
            session.sse = writer;
            new Thread(() -> streamSse(session, writer)).start();
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (!checkOrigin(req, resp)) return;
            Session session = getSession(req, resp, false);
            if (session != null) {
                sessionRef.set(null);
                resp.setStatus(HttpServletResponse.SC_OK);
            }
        }

        private Session getSession(HttpServletRequest req, HttpServletResponse resp, boolean create) {
            Session session = sessionRef.get();
            String id = req.getHeader("Mcp-Session-Id");
            if (session == null && create) {
                String newId = UUID.randomUUID().toString();
                session = new Session(newId);
                sessionRef.set(session);
                resp.setHeader("Mcp-Session-Id", newId);
            } else if (session != null && (id == null || !session.id.equals(id))) {
                session = null;
            }
            if (session == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
            return session;
        }

        private JsonObject take(BlockingQueue<JsonObject> q) {
            try {
                return q.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        private void streamSse(Session session, PrintWriter writer) {
            try {
                while (true) {
                    JsonObject obj = take(session.outbound);
                    writer.write("data: "+obj.toString()+"\n\n");
                    writer.flush();
                }
            } catch (Exception ignore) {
            } finally {
                writer.close();
                session.sse = null;
            }
        }

        private boolean checkOrigin(HttpServletRequest req, HttpServletResponse resp) {
            String origin = req.getHeader("Origin");
            if (origin != null && !origin.contains("localhost")) {
                resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return false;
            }
            return true;
        }
    }
}
