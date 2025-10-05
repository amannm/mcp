package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.RequestMethod;
import com.amannmalik.mcp.jsonrpc.JsonRpcEnvelope;
import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParsingException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.Serial;
import java.lang.System.Logger;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/// - [Transports](specification/2025-06-18/basic/transports.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
final class McpServlet extends HttpServlet {
    /// Explicit serialVersionUID for stable servlet serialization semantics.
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = PlatformLog.get(McpServlet.class);

    private transient final StreamableHttpServerTransport transport;
    private final int responseQueueCapacity;

    McpServlet(StreamableHttpServerTransport transport, int responseQueueCapacity) {
        this.transport = Objects.requireNonNull(transport, "transport");
        if (responseQueueCapacity <= 0) {
            throw new IllegalArgumentException("responseQueueCapacity must be positive");
        }
        this.responseQueueCapacity = responseQueueCapacity;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var principalOpt = enforceHttpsAndAuthorize(req, resp, true, true);
        if (principalOpt.isEmpty()) {
            return;
        }
        var principal = principalOpt.get();

        var payload = readJson(req, resp);
        if (payload.isEmpty()) {
            return;
        }
        var obj = payload.get();
        var envelope = JsonRpcEnvelope.of(obj);
        var initializing = envelope.method()
                .map(RequestMethod.INITIALIZE.method()::equals)
                .orElse(false);

        if (!transport.validateSession(req, resp, principal, initializing)) {
            return;
        }

        switch (envelope.type()) {
            case NOTIFICATION, RESPONSE -> enqueue(obj, resp);
            case REQUEST -> handleRequest(obj, envelope, initializing, req, resp);
            default -> resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var principalOpt = enforceHttpsAndAuthorize(req, resp, true, false);
        if (principalOpt.isEmpty()) {
            return;
        }
        if (!transport.validateSession(req, resp, principalOpt.get(), false)) {
            return;
        }
        var ac = initSse(req, resp);
        transport.registerGeneralClient(ac, req.getHeader("Last-Event-ID"));
        transport.flushBacklog();
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        var principalOpt = enforceHttpsAndAuthorize(req, resp, false, false);
        if (principalOpt.isEmpty()) {
            return;
        }
        if (!transport.validateSession(req, resp, principalOpt.get(), false)) {
            return;
        }
        transport.terminateSession(true);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private Optional<Principal> enforceHttpsAndAuthorize(HttpServletRequest req,
                                                         HttpServletResponse resp,
                                                         boolean requireAccept,
                                                         boolean post) throws IOException {
        if (!transport.enforceHttps(req, resp)) {
            return Optional.empty();
        }
        return authorize(req, resp, requireAccept, post);
    }

    private Optional<Principal> authorize(HttpServletRequest req,
                                          HttpServletResponse resp,
                                          boolean requireAccept,
                                          boolean post) throws IOException {
        var principalOpt = transport.authorize(req, resp);
        if (principalOpt.isEmpty()) {
            return Optional.empty();
        }
        if (!transport.verifyOrigin(req, resp)) {
            return Optional.empty();
        }
        if (requireAccept && !transport.validateAccept(req, resp, post)) {
            return Optional.empty();
        }
        return principalOpt;
    }

    private Optional<JsonObject> readJson(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try (var reader = Json.createReader(req.getInputStream())) {
            return Optional.of(reader.readObject());
        } catch (JsonParsingException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return Optional.empty();
        }
    }

    private AsyncContext initSse(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/event-stream;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        applySessionHeaders(resp);
        resp.flushBuffer();
        var ac = req.startAsync();
        ac.setTimeout(0);
        return ac;
    }

    private void enqueue(JsonObject obj, HttpServletResponse resp) throws IOException {
        try {
            transport.submitIncoming(obj);
            resp.setStatus(HttpServletResponse.SC_ACCEPTED);
            applySessionHeaders(resp);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }

    private void applySessionHeaders(HttpServletResponse resp) {
        transport.sessionId().ifPresent(id -> resp.setHeader(TransportHeaders.SESSION_ID, id));
        resp.setHeader(TransportHeaders.PROTOCOL_VERSION, transport.protocolVersion());
    }

    private void handleRequest(JsonObject obj,
                               JsonRpcEnvelope envelope,
                               boolean initializing,
                               HttpServletRequest req,
                               HttpServletResponse resp) throws IOException {
        if (initializing) {
            handleInitialize(obj, envelope, resp);
        } else {
            handleStreamRequest(obj, envelope, req, resp);
        }
    }

    private void handleInitialize(JsonObject obj,
                                  JsonRpcEnvelope envelope,
                                  HttpServletResponse resp) throws IOException {
        var id = envelope.requireId();
        BlockingQueue<JsonObject> queue = transport.registerResponseQueue(id, responseQueueCapacity);
        try {
            transport.submitIncoming(obj);
            var timeoutSeconds = transport.initializeRequestTimeout().toSeconds();
            var response = queue.poll(timeoutSeconds, TimeUnit.SECONDS);
            if (response == null) {
                resp.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
                return;
            }
            if (response.containsKey("result")) {
                var result = response.getJsonObject("result");
                if (result.containsKey("protocolVersion")) {
                    transport.updateProtocolVersion(result.getString("protocolVersion"));
                }
            }
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.setHeader(TransportHeaders.PROTOCOL_VERSION, transport.protocolVersion());
            resp.getWriter().write(response.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } finally {
            transport.removeResponseQueue(id);
        }
    }

    private void handleStreamRequest(JsonObject obj,
                                     JsonRpcEnvelope envelope,
                                     HttpServletRequest req,
                                     HttpServletResponse resp) throws IOException {
        var ac = initSse(req, resp);
        var key = envelope.requireId();
        SseClient client = transport.registerRequestClient(key, ac);
        try {
            transport.submitIncoming(obj);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            transport.unregisterRequestClient(key, client);
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        }
    }
}
