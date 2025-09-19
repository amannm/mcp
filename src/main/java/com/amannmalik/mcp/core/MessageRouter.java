package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.jsonrpc.JsonRpcEnvelope;
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

    public RouteOutcome route(JsonObject message) {
        Objects.requireNonNull(message, "message");

        var envelope = JsonRpcEnvelope.of(message);
        return envelope.id()
                .map(id -> routeWithId(envelope, id))
                .orElseGet(() -> routeToGeneralClients(envelope.message()));
    }

    private RouteOutcome routeWithId(JsonRpcEnvelope envelope, RequestId id) {
        var outcome = routeById(id, envelope);
        if (outcome != RouteOutcome.NOT_FOUND) {
            return outcome;
        }
        return switch (envelope.type()) {
            case REQUEST, NOTIFICATION -> routeToGeneralClients(envelope.message());
            case RESPONSE, INVALID -> RouteOutcome.NOT_FOUND;
        };
    }

    private RouteOutcome routeById(RequestId id, JsonRpcEnvelope envelope) {
        var message = envelope.message();
        var streamOutcome = sendToRequestStream(id, message, envelope);
        if (streamOutcome != RouteOutcome.NOT_FOUND) {
            return streamOutcome;
        }
        return sendToResponseQueue(id, message);
    }

    private RouteOutcome routeToGeneralClients(JsonObject message) {
        if (sendToActiveClient(message) || sendToPending(message)) {
            return RouteOutcome.DELIVERED;
        }
        return RouteOutcome.PENDING;
    }

    private RouteOutcome sendToRequestStream(RequestId id, JsonObject message, JsonRpcEnvelope envelope) {
        var stream = requestStreams.apply(id);
        if (stream == null) {
            return RouteOutcome.NOT_FOUND;
        }
        stream.send(message);
        if (envelope.isResponse()) {
            remover.accept(id, stream);
        }
        return RouteOutcome.DELIVERED;
    }

    private RouteOutcome sendToResponseQueue(RequestId id, JsonObject message) {
        var q = responseQueues.apply(id);
        if (q == null) {
            return RouteOutcome.NOT_FOUND;
        }
        q.add(message);
        return RouteOutcome.DELIVERED;
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

