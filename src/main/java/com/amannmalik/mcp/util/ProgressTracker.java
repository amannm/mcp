package com.amannmalik.mcp.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressTracker {
    private final Map<ProgressToken, Double> progress = new ConcurrentHashMap<>();

    public void register(ProgressToken token) {
        Double prev = progress.putIfAbsent(token, Double.NEGATIVE_INFINITY);
        if (prev != null) {
            throw new IllegalArgumentException("Duplicate token: " + token);
        }
    }

    public void release(ProgressToken token) {
        progress.remove(token);
    }

    public boolean isActive(ProgressToken token) {
        return progress.containsKey(token);
    }

    public boolean hasProgress(ProgressToken token) {
        Double p = progress.get(token);
        return p != null && p > Double.NEGATIVE_INFINITY;
    }

    public void update(ProgressNotification note) {
        progress.compute(note.token(), (t, prev) -> {
            if (prev == null) {
                throw new IllegalStateException("Unknown progress token: " + t);
            }
            if (note.progress() <= prev) {
                throw new IllegalArgumentException("progress must increase");
            }
            return note.progress();
        });
    }
}
