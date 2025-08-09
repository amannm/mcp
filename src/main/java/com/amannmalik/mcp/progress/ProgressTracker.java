package com.amannmalik.mcp.progress;

import java.util.*;

public final class ProgressTracker {
    private final Map<String, List<Progress>> updates = new HashMap<>();
    private final Set<String> active = new HashSet<>();
    private final Set<String> cancelled = new HashSet<>();

    public String start(String token) {
        active.add(token);
        updates.put(token, new ArrayList<>());
        return token;
    }

    public void notify(String token, double progress, int total, String message) {
        updates.computeIfAbsent(token, t -> new ArrayList<>())
                .add(new Progress(progress, total, message));
    }

    public List<Progress> progress(String token) {
        return updates.getOrDefault(token, List.of());
    }

    public void cancel(String token, String reason) {
        cancelled.add(token);
    }

    public boolean cancelled(String token) {
        return cancelled.contains(token);
    }

    public void release(String token) {
        active.remove(token);
    }

    public boolean active(String token) {
        return active.contains(token);
    }

    public record Progress(double progress, int total, String message) {
    }
}
