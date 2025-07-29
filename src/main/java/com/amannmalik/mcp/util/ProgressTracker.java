package com.amannmalik.mcp.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks active progress tokens to ensure uniqueness. */
public final class ProgressTracker {
    private final Set<ProgressToken> active = ConcurrentHashMap.newKeySet();
    private final Map<ProgressToken, Double> progress = new ConcurrentHashMap<>();

    /** Registers a token, throwing if already active. */
    public void register(ProgressToken token) {
        if (!active.add(token)) {
            throw new IllegalArgumentException("Duplicate token: " + token);
        }
        progress.put(token, Double.NEGATIVE_INFINITY);
    }

    /** Releases a token once the request completes. */
    public void release(ProgressToken token) {
        active.remove(token);
        progress.remove(token);
    }

    /** Returns true if the token is currently registered. */
    public boolean isActive(ProgressToken token) {
        return active.contains(token);
    }

    /**
     * Records progress for the given token, ensuring it increases.
     */
    public void update(ProgressNotification note) {
        ProgressToken token = note.token();
        if (!active.contains(token)) {
            throw new IllegalStateException("Unknown progress token: " + token);
        }
        double prev = progress.get(token);
        if (note.progress() <= prev) {
            throw new IllegalArgumentException("progress must increase");
        }
        progress.put(token, note.progress());
    }
}
