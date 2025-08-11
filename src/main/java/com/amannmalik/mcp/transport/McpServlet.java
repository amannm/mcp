package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.*;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.*;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
final class McpServlet extends HttpServlet {
    private final StreamableHttpServerTransport transport;

    McpServlet(StreamableHttpServerTransport transport) {
        this.transport = transport;
    }

    private static MessageType classify(JsonObject obj) {
        boolean hasMethod = obj.containsKey("method");
        boolean hasId = obj.containsKey("id");
        boolean isRequest = hasMethod && hasId;
        boolean isNotification = hasMethod && !hasId;
        boolean isResponse = !hasMethod && (obj.containsKey("result") || obj.containsKey("error"));
        if (isRequest) return MessageType.REQUEST;
        if (isNotification) return MessageType.NOTIFICATION;
        if (isResponse) return MessageType.RESPONSE;
        return MessageType.INVALID;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var principalOpt = authorize(req, resp, true, true);
        if (principalOpt.isEmpty()) return;
        var principal = principalOpt.get();

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

        switch (classify(obj)) {
            case NOTIFICATION, RESPONSE -> enqueue(obj, resp);
            case REQUEST -> handleRequest(obj, initializing, req, resp);
            default -> resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var principalOpt = authorize(req, resp, true, false);
        if (principalOpt.isEmpty()) return;
        if (!transport.validateSession(req, resp, principalOpt.get(), false)) return;
        AsyncContext ac = initSse(req, resp);

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
                found = transport.clients.byPrefix.get(prefix);
                if (found != null) {
                    found.attach(ac, lastId);
                }
            }
        }
        SseClient client;
        if (found == null) {
            client = new SseClient(ac);
            transport.clients.byPrefix.put(client.prefix, client);
        } else {
            client = found;
            transport.clients.lastGeneral.set(null);
        }
        transport.clients.general.add(client);
        transport.flushBacklog();
        ac.addListener(transport.clients.generalListener(client));
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var principalOpt = authorize(req, resp, false, false);
        if (principalOpt.isEmpty()) return;
        if (!transport.validateSession(req, resp, principalOpt.get(), false)) return;
        transport.terminateSession(true);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private Optional<Principal> authorize(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          boolean requireAccept,
                                          boolean post) throws IOException {
        var principalOpt = transport.authorize(req, resp);
        if (principalOpt.isEmpty()) return Optional.empty();
        if (!transport.verifyOrigin(req, resp)) return Optional.empty();
        if (requireAccept && !transport.validateAccept(req, resp, post)) return Optional.empty();
        return principalOpt;
    }

    private AsyncContext initSse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/event-stream;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader(TransportHeaders.PROTOCOL_VERSION, transport.sessions.protocolVersion());
        resp.flushBuffer();
        AsyncContext ac = req.startAsync();
        ac.setTimeout(0);
        return ac;
    }

    private void enqueue(JsonObject obj, HttpServletResponse resp) throws IOException {
        try {
            transport.incoming.put(obj);
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    private void handleRequest(JsonObject obj,
                               boolean initializing,
                               HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        if (initializing) {
            handleInitialize(obj, resp);
        } else {
            handleStreamRequest(obj, req, resp);
        }
    }

    private void handleInitialize(JsonObject obj, HttpServletResponse resp) throws IOException {
        String id = obj.get("id").toString();
        BlockingQueue<JsonObject> q = new LinkedBlockingQueue<>(
                McpConfiguration.current().responseQueueCapacity());
        transport.clients.responses.put(id, q);
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
            transport.clients.responses.remove(id);
        }
    }

    private void handleStreamRequest(JsonObject obj,
                                     HttpServletRequest req,
                                     HttpServletResponse resp) throws IOException {
        AsyncContext ac = initSse(req, resp);
        SseClient client = new SseClient(ac);
        String key = obj.get("id").toString();
        transport.clients.request.put(key, client);
        transport.clients.byPrefix.put(client.prefix, client);
        ac.addListener(transport.clients.requestListener(key, client));
        try {
            transport.incoming.put(obj);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            transport.clients.removeRequest(key, client);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    private enum MessageType {REQUEST, NOTIFICATION, RESPONSE, INVALID}
}

