package com.amannmalik.mcp.jsonrpc;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class PendingRequests {
    private final AtomicLong counter = new AtomicLong(1);
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();

    public record Context(RequestId id, CompletableFuture<JsonRpcMessage> future) {}

    public RequestId nextId() {
        return new RequestId.NumericId(counter.getAndIncrement());
    }

    public Context register() {
        var id = nextId();
        var future = new CompletableFuture<JsonRpcMessage>();
        pending.put(id, future);
        return new Context(id, future);
    }

    public void complete(RequestId id, JsonRpcMessage msg) {
        var f = pending.remove(id);
        if (f != null) f.complete(msg);
    }

    public void remove(RequestId id) {
        pending.remove(id);
    }

    public void failAll(IOException e) {
        pending.values().forEach(f -> f.completeExceptionally(e));
        pending.clear();
    }
}

