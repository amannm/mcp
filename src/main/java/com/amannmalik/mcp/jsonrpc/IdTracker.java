package com.amannmalik.mcp.jsonrpc;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks request IDs used within a session to ensure uniqueness. */
public final class IdTracker {
    private final Set<RequestId> seen = ConcurrentHashMap.newKeySet();

    /**
     * Registers the ID, throwing if it was already seen.
     */
    public void register(RequestId id) {
        if (!seen.add(id)) {
            throw new IllegalArgumentException("Duplicate id: " + id);
        }
    }
}
