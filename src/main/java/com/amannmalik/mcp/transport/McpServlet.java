package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonParsingException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

final class McpServlet extends HttpServlet {
    private final StreamableHttpTransport transport;

    McpServlet(StreamableHttpTransport transport) {
        this.transport = transport;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Principal principal = transport.authorize(req, resp);
        if (principal == null && transport.authManager != null) return;
        if (!transport.verifyOrigin(req, resp)) return;
        if (!transport.validateAccept(req, resp, true)) return;

        JsonObject obj;
        try (JsonReader reader = Json.createReader(req.getInputStream())) {
            obj = reader.readObject();
        } catch (JsonParsingException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        boolean initializing = RequestMethod.INITIALIZE.method()
                .equals(obj.getString("method", null));

        if (!transport.validateSession(req, resp, principal, initializing)) return;

        boolean hasMethod = obj.containsKey("method");
        boolean hasId = obj.containsKey("id");
        boolean isRequest = hasMethod && hasId;
        boolean isNotification = hasMethod && !hasId;
        boolean isResponse = !hasMethod && (obj.containsKey("result") || obj.containsKey("error"));

        if (isNotification || isResponse) {
            try {
                transport.incoming.put(obj);
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
            transport.responseQueues.put(obj.get("id").toString(), q);
            try {
                transport.incoming.put(obj);
                JsonObject response = q.poll(30, TimeUnit.SECONDS);
                if (response == null) {
                    resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                    return;
                }
                if (response.containsKey("result")) {
                    JsonObject result = response.getJsonObject("result");
                    if (result.containsKey("protocolVersion")) {
                        transport.sessions.protocolVersion(result.getString("protocolVersion"));
                    }
                }
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.setHeader(TransportHeaders.PROTOCOL_VERSION, transport.sessions.protocolVersion());
                resp.getWriter().write(response.toString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } finally {
                transport.responseQueues.remove(obj.get("id").toString());
            }
        } else {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/event-stream;charset=UTF-8");
            resp.setHeader("Cache-Control", "no-cache");
            resp.setHeader(TransportHeaders.PROTOCOL_VERSION, transport.sessions.protocolVersion());
            resp.flushBuffer();
            AsyncContext ac = req.startAsync();
            ac.setTimeout(0);
            SseClient client = new SseClient(ac);
            String key = obj.get("id").toString();
            transport.requestStreams.put(key, client);
            transport.clientsByPrefix.put(client.prefix, client);
            ac.addListener(transport.requestStreamListener(key, client));
            try {
                transport.incoming.put(obj);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                transport.removeRequestStream(key, client);
                resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Principal principal = transport.authorize(req, resp);
        if (principal == null && transport.authManager != null) return;
        if (!transport.verifyOrigin(req, resp)) return;
        if (!transport.validateAccept(req, resp, false)) return;
        if (!transport.validateSession(req, resp, principal, false)) return;
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/event-stream;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader(TransportHeaders.PROTOCOL_VERSION, transport.sessions.protocolVersion());
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
                found = transport.clientsByPrefix.get(prefix);
                if (found != null) {
                    found.attach(ac, lastId);
                }
            }
        }
        SseClient client;
        if (found == null) {
            client = new SseClient(ac);
            transport.clientsByPrefix.put(client.prefix, client);
        } else {
            client = found;
            transport.lastGeneral.set(null);
        }
        transport.generalClients.add(client);
        ac.addListener(transport.generalStreamListener(client));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Principal principal = transport.authorize(req, resp);
        if (principal == null && transport.authManager != null) return;
        if (!transport.verifyOrigin(req, resp)) return;
        if (!transport.validateSession(req, resp, principal, false)) return;
        transport.terminateSession(true);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}

