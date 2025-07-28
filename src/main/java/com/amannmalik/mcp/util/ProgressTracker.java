package com.amannmalik.mcp.util;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks active progress tokens to ensure uniqueness. */
public final class ProgressTracker {
    private final Set<ProgressToken> active = ConcurrentHashMap.newKeySet();

    /** Registers a token, throwing if already active. */
    public void register(ProgressToken token) {
        if (!active.add(token)) {
            throw new IllegalArgumentException("Duplicate token: " + token);
        }
    }

    /** Releases a token once the request completes. */
    public void release(ProgressToken token) {
        active.remove(token);
    }
}
