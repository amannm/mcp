package com.amannmalik.mcp.jsonrpc;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public final class IdTracker {
    private final Set<RequestId> seen = ConcurrentHashMap.newKeySet();

    
    public void register(RequestId id) {
        if (!seen.add(id)) {
            throw new IllegalArgumentException("Duplicate id: " + id);
        }
    }

    
    public void release(RequestId id) {
        seen.remove(id);
    }
}
