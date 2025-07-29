package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class CancellationTracker {
    private final Set<RequestId> active = ConcurrentHashMap.newKeySet();
    private final Map<RequestId, String> cancelled = new ConcurrentHashMap<>();


    public void register(RequestId id) {
        if (!active.add(id)) {
            throw new IllegalArgumentException("Duplicate request: " + id);
        }
    }


    public void cancel(RequestId id, String reason) {
        if (active.contains(id)) {
            cancelled.put(id, reason);
        }
    }


    public boolean isCancelled(RequestId id) {
        return cancelled.containsKey(id);
    }


    public String reason(RequestId id) {
        return cancelled.get(id);
    }


    public void release(RequestId id) {
        active.remove(id);
        cancelled.remove(id);
    }
}
