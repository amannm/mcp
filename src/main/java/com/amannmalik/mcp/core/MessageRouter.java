package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.jsonrpc.JsonRpcEnvelope;
import com.amannmalik.mcp.transport.SseClient;
import jakarta.json.JsonObject;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

/// - [Base Protocol](specification/2025-06-18/basic/index.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
public final class MessageRouter {
    private final Routes routes;

    public MessageRouter(Routes routes) {
        this.routes = Objects.requireNonNull(routes, "routes");
    }

    public RouteOutcome route(JsonObject message) {
        Objects.requireNonNull(message, "message");
        var envelope = JsonRpcEnvelope.of(message);
        return switch (envelope.type()) {
            case REQUEST -> routeRequest(envelope);
            case NOTIFICATION -> deliverToGeneralClients(envelope.message());
            case RESPONSE -> routeResponse(envelope);
            case INVALID -> routeInvalid(envelope);
        };
    }

    private RouteOutcome routeRequest(JsonRpcEnvelope envelope) {
        return envelope.id()
                .flatMap(id -> attemptIdentifiedRoutes(id, envelope))
                .orElseGet(() -> deliverToGeneralClients(envelope.message()));
    }

    private RouteOutcome routeResponse(JsonRpcEnvelope envelope) {
        return envelope.id()
                .flatMap(id -> attemptIdentifiedRoutes(id, envelope))
                .orElse(RouteOutcome.NOT_FOUND);
    }

    private RouteOutcome routeInvalid(JsonRpcEnvelope envelope) {
        return envelope.id()
                .map(id -> attemptIdentifiedRoutes(id, envelope).orElse(RouteOutcome.NOT_FOUND))
                .orElseGet(() -> deliverToGeneralClients(envelope.message()));
    }

    private Optional<RouteOutcome> attemptIdentifiedRoutes(RequestId id, JsonRpcEnvelope envelope) {
        return attemptRequestStream(id, envelope)
                .or(() -> attemptResponseQueue(id, envelope.message()));
    }

    private Optional<RouteOutcome> attemptRequestStream(RequestId id, JsonRpcEnvelope envelope) {
        return routes.requestClient(id)
                .map(client -> deliverToRequestStream(id, envelope, client));
    }

    private RouteOutcome deliverToRequestStream(RequestId id, JsonRpcEnvelope envelope, SseClient client) {
        var message = envelope.message();
        if (!deliverToClientIfActive(id, client, message)) {
            return RouteOutcome.NOT_FOUND;
        }
        if (envelope.isResponse()) {
            routes.removeRequestClient(id, client);
        }
        return RouteOutcome.DELIVERED;
    }

    private boolean deliverToClientIfActive(RequestId id, SseClient client, JsonObject message) {
        if (evictIfInactive(id, client)) {
            return false;
        }
        client.send(message);
        return !evictIfInactive(id, client);
    }

    private boolean evictIfInactive(RequestId id, SseClient client) {
        if (!client.isActive()) {
            routes.removeRequestClient(id, client);
            return true;
        }
        return false;
    }

    private Optional<RouteOutcome> attemptResponseQueue(RequestId id, JsonObject message) {
        return routes.takeResponseQueue(id)
                .map(queue -> {
                    queue.add(message);
                    return RouteOutcome.DELIVERED;
                });
    }

    private RouteOutcome deliverToGeneralClients(JsonObject message) {
        if (sendToActiveClients(message) || deliverToPendingClient(message)) {
            return RouteOutcome.DELIVERED;
        }
        return RouteOutcome.PENDING;
    }

    private boolean sendToActiveClients(JsonObject message) {
        var delivered = false;
        for (var client : routes.generalClients()) {
            if (sendIfActive(client, message)) {
                delivered = true;
            }
        }
        return delivered;
    }

    private boolean deliverToPendingClient(JsonObject message) {
        return routes.pendingGeneralClient()
                .map(client -> sendIfActive(client, message))
                .orElse(false);
    }

    private boolean sendIfActive(SseClient client, JsonObject message) {
        if (!client.isActive()) {
            return false;
        }
        client.send(message);
        return true;
    }

    public interface Routes {
        Optional<SseClient> requestClient(RequestId id);

        Optional<BlockingQueue<JsonObject>> takeResponseQueue(RequestId id);

        Iterable<SseClient> generalClients();

        Optional<SseClient> pendingGeneralClient();

        void removeRequestClient(RequestId id, SseClient client);
    }
}
