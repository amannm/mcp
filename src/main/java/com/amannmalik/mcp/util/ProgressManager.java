package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.wire.NotificationMethod;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressManager {
    private final Map<ProgressToken, Double> progress = new ConcurrentHashMap<>();
    private final Map<RequestId, ProgressToken> tokens = new ConcurrentHashMap<>();
    private final Set<RequestId> active = ConcurrentHashMap.newKeySet();
    private final Map<RequestId, String> cancelled = new ConcurrentHashMap<>();
    private final RateLimiter limiter;

    public ProgressManager(RateLimiter limiter) {
        if (limiter == null) throw new IllegalArgumentException("limiter required");
        this.limiter = limiter;
    }

    public Optional<ProgressToken> register(RequestId id, JsonObject params) {
        if (!active.add(id)) throw new IllegalArgumentException("Duplicate request: " + id);
        Optional<ProgressToken> token = ProgressNotification.fromMeta(params);
        token.ifPresent(t -> {
            Double prev = progress.putIfAbsent(t, Double.NEGATIVE_INFINITY);
            if (prev != null) throw new IllegalArgumentException("Duplicate token: " + t);
            tokens.put(id, t);
        });
        return token;
    }

    public void release(RequestId id) {
        active.remove(id);
        cancelled.remove(id);
        ProgressToken t = tokens.remove(id);
        if (t != null) progress.remove(t);
    }

    public void cancel(RequestId id, String reason) {
        if (active.contains(id)) cancelled.put(id, reason);
    }

    public boolean isCancelled(RequestId id) {
        return cancelled.containsKey(id);
    }

    public String reason(RequestId id) {
        return cancelled.get(id);
    }

    public void record(ProgressNotification note) {
        update(note);
        if (note.progress() >= 1.0) {
            progress.remove(note.token());
            tokens.values().removeIf(t -> t.equals(note.token()));
        }
    }

    private void update(ProgressNotification note) {
        progress.compute(note.token(), (t, prev) -> {
            if (prev == null) throw new IllegalStateException("Unknown progress token: " + t);
            if (note.progress() <= prev) throw new IllegalArgumentException("progress must increase");
            return note.progress();
        });
    }

    private boolean isActive(ProgressToken token) {
        return progress.containsKey(token);
    }

    public boolean hasProgress(ProgressToken token) {
        Double p = progress.get(token);
        return p != null && p > Double.NEGATIVE_INFINITY;
    }

    public void send(ProgressNotification note, NotificationSender sender) throws IOException {
        if (!isActive(note.token())) return;
        try {
            limiter.requireAllowance(note.token().asString());
            update(note);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ignore) {
            return;
        }
        sender.send(new JsonRpcNotification(
                NotificationMethod.PROGRESS.method(),
                ProgressNotification.CODEC.toJson(note)
        ));
    }
}
