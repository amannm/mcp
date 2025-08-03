package com.amannmalik.mcp.transport;

import jakarta.json.JsonObject;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/// - [Base Protocol](specification/2025-06-18/basic/index.mdx)
/// - [MCP Conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:6-34)
final class MessageRouter {
    private final ConcurrentHashMap<String, SseClient> requestStreams;
    private final ConcurrentHashMap<String, BlockingQueue<JsonObject>> responseQueues;
    private final Set<SseClient> generalClients;
    private final AtomicReference<SseClient> lastGeneral;
    private final BiConsumer<String, SseClient> remover;

    MessageRouter(ConcurrentHashMap<String, SseClient> requestStreams,
                  ConcurrentHashMap<String, BlockingQueue<JsonObject>> responseQueues,
                  Set<SseClient> generalClients,
                  AtomicReference<SseClient> lastGeneral,
                  BiConsumer<String, SseClient> remover) {
        this.requestStreams = requestStreams;
        this.responseQueues = responseQueues;
        this.generalClients = generalClients;
        this.lastGeneral = lastGeneral;
        this.remover = remover;
    }

    boolean route(JsonObject message) {
        String id = message.containsKey("id") ? message.get("id").toString() : null;
        String method = message.getString("method", null);
        if (id != null) {
            if (sendToRequestStream(id, method, message)) return true;
            if (sendToResponseQueue(id, message)) return true;
            if (method == null) return false;
        }
        if (sendToActiveClient(message)) return true;
        return sendToPending(message);
    }

    private boolean sendToRequestStream(String id, String method, JsonObject message) {
        SseClient stream = requestStreams.get(id);
        if (stream == null) return false;
        stream.send(message);
        if (method == null) remover.accept(id, stream);
        return true;
    }

    private boolean sendToResponseQueue(String id, JsonObject message) {
        var q = responseQueues.remove(id);
        if (q == null) return false;
        q.add(message);
        return true;
    }

    private boolean sendToActiveClient(JsonObject message) {
        for (SseClient c : generalClients) {
            if (c.isActive()) {
                c.send(message);
                return true;
            }
        }
        return false;
    }

    private boolean sendToPending(JsonObject message) {
        SseClient pending = lastGeneral.get();
        if (pending == null) return false;
        pending.send(message);
        return true;
    }
}

