package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.transport.SseClient;
import jakarta.json.JsonObject;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/// - [Base Protocol](specification/2025-06-18/basic/index.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
public final class MessageRouter {
    private final Function<RequestId, SseClient> requestStreams;
    private final Function<RequestId, BlockingQueue<JsonObject>> responseQueues;
    private final Supplier<Iterable<SseClient>> generalClients;
    private final Supplier<SseClient> lastGeneral;
    private final BiConsumer<RequestId, SseClient> remover;

    public MessageRouter(Function<RequestId, SseClient> requestStreams,
                         Function<RequestId, BlockingQueue<JsonObject>> responseQueues,
                         Supplier<Iterable<SseClient>> generalClients,
                         Supplier<SseClient> lastGeneral,
                         BiConsumer<RequestId, SseClient> remover) {
        this.requestStreams = Objects.requireNonNull(requestStreams, "requestStreams");
        this.responseQueues = Objects.requireNonNull(responseQueues, "responseQueues");
        this.generalClients = Objects.requireNonNull(generalClients, "generalClients");
        this.lastGeneral = Objects.requireNonNull(lastGeneral, "lastGeneral");
        this.remover = Objects.requireNonNull(remover, "remover");
    }

    public boolean route(JsonObject message) {
        var idValue = RequestId.fromNullable(message.get("id"));
        var method = message.getString("method", null);
        if (idValue.isPresent()) {
            var id = idValue.get();
            if (sendToRequestStream(id, method, message)) {
                return true;
            }
            if (sendToResponseQueue(id, message)) {
                return true;
            }
            if (method == null) {
                return false;
            }
        }
        if (sendToActiveClient(message)) {
            return true;
        }
        return sendToPending(message);
    }

    private boolean sendToRequestStream(RequestId id, String method, JsonObject message) {
        var stream = requestStreams.apply(id);
        if (stream == null) {
            return false;
        }
        stream.send(message);
        if (method == null) {
            remover.accept(id, stream);
        }
        return true;
    }

    private boolean sendToResponseQueue(RequestId id, JsonObject message) {
        var q = responseQueues.apply(id);
        if (q == null) {
            return false;
        }
        q.add(message);
        return true;
    }

    private boolean sendToActiveClient(JsonObject message) {
        for (var c : generalClients.get()) {
            if (c.isActive()) {
                c.send(message);
                return true;
            }
        }
        return false;
    }

    private boolean sendToPending(JsonObject message) {
        var pending = lastGeneral.get();
        if (pending == null) {
            return false;
        }
        pending.send(message);
        return true;
    }
}

