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
        return envelope.id()
                .map(id -> routeWithId(envelope, id))
                .orElseGet(() -> routeToGeneralClients(envelope.message()));
    }

    private RouteOutcome routeWithId(JsonRpcEnvelope envelope, RequestId id) {
        var outcome = routeById(id, envelope);
        return outcome == RouteOutcome.NOT_FOUND ? handleUnroutableWithId(envelope) : outcome;
    }

    private RouteOutcome handleUnroutableWithId(JsonRpcEnvelope envelope) {
        return switch (envelope.type()) {
            case REQUEST, NOTIFICATION -> routeToGeneralClients(envelope.message());
            case RESPONSE, INVALID -> RouteOutcome.NOT_FOUND;
        };
    }

    private RouteOutcome routeById(RequestId id, JsonRpcEnvelope envelope) {
        return attemptRequestStream(id, envelope)
                .or(() -> attemptResponseQueue(id, envelope.message()))
                .orElse(RouteOutcome.NOT_FOUND);
    }

    private Optional<RouteOutcome> attemptRequestStream(RequestId id, JsonRpcEnvelope envelope) {
        return routes.requestClient(id)
                .map(client -> deliverToRequestStream(id, envelope, client));
    }

    private RouteOutcome deliverToRequestStream(RequestId id, JsonRpcEnvelope envelope, SseClient client) {
        var message = envelope.message();
        if (!client.isActive()) {
            routes.removeRequestClient(id, client);
            return RouteOutcome.NOT_FOUND;
        }
        client.send(message);
        if (!client.isActive()) {
            routes.removeRequestClient(id, client);
            return RouteOutcome.NOT_FOUND;
        }
        if (envelope.isResponse()) {
            routes.removeRequestClient(id, client);
        }
        return RouteOutcome.DELIVERED;
    }

    private Optional<RouteOutcome> attemptResponseQueue(RequestId id, JsonObject message) {
        return routes.takeResponseQueue(id)
                .map(queue -> {
                    queue.add(message);
                    return RouteOutcome.DELIVERED;
                });
    }

    private RouteOutcome routeToGeneralClients(JsonObject message) {
        if (sendToActiveClients(message) || deliverToPendingClient(message)) {
            return RouteOutcome.DELIVERED;
        }
        return RouteOutcome.PENDING;
    }

    private boolean sendToActiveClients(JsonObject message) {
        var delivered = false;
        for (var client : routes.generalClients()) {
            if (!client.isActive()) {
                continue;
            }
            client.send(message);
            delivered = true;
        }
        return delivered;
    }

    private boolean deliverToPendingClient(JsonObject message) {
        return routes.pendingGeneralClient()
                .filter(SseClient::isActive)
                .map(client -> {
                    client.send(message);
                    return true;
                })
                .orElse(false);
    }

    public interface Routes {
        Optional<SseClient> requestClient(RequestId id);

        Optional<BlockingQueue<JsonObject>> takeResponseQueue(RequestId id);

        Iterable<SseClient> generalClients();

        Optional<SseClient> pendingGeneralClient();

        void removeRequestClient(RequestId id, SseClient client);
    }
}

