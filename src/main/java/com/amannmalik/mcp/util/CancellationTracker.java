package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks active requests and cancellations. */
public final class CancellationTracker {
    private final Set<RequestId> active = ConcurrentHashMap.newKeySet();
    private final Map<RequestId, String> cancelled = new ConcurrentHashMap<>();

    /** Registers a request id, throwing if already active. */
    public void register(RequestId id) {
        if (!active.add(id)) {
            throw new IllegalArgumentException("Duplicate request: " + id);
        }
    }

    /** Marks a request as cancelled. */
    public void cancel(RequestId id, String reason) {
        if (active.contains(id)) {
            cancelled.put(id, reason);
        }
    }

    /** Returns true if the request has been cancelled. */
    public boolean isCancelled(RequestId id) {
        return cancelled.containsKey(id);
    }

    /** Retrieves the cancellation reason or null. */
    public String reason(RequestId id) {
        return cancelled.get(id);
    }

    /** Releases a request once processing finishes. */
    public void release(RequestId id) {
        active.remove(id);
        cancelled.remove(id);
    }
}
