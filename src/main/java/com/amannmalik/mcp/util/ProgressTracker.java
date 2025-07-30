package com.amannmalik.mcp.util;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressTracker {
    private final Set<ProgressToken> active = ConcurrentHashMap.newKeySet();
    private final Map<ProgressToken, Double> progress = new ConcurrentHashMap<>();

    public void register(ProgressToken token) {
        if (!active.add(token)) {
            throw new IllegalArgumentException("Duplicate token: " + token);
        }
        progress.put(token, Double.NEGATIVE_INFINITY);
    }

    public void release(ProgressToken token) {
        active.remove(token);
        progress.remove(token);
    }

    public boolean isActive(ProgressToken token) {
        return active.contains(token);
    }

    public boolean hasProgress(ProgressToken token) {
        Double p = progress.get(token);
        return p != null && p > Double.NEGATIVE_INFINITY;
    }

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
