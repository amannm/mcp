package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.codec.ProgressNotificationJsonCodec;
import com.amannmalik.mcp.util.NotificationSender;
import com.amannmalik.mcp.util.RateLimiter;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressManager {
    private final Map<ProgressToken, Double> progress = new ConcurrentHashMap<>();
    private final Map<RequestId, ProgressToken> tokens = new ConcurrentHashMap<>();
    private final Set<RequestId> active = ConcurrentHashMap.newKeySet();
    private final Set<RequestId> used = ConcurrentHashMap.newKeySet();
    private final Map<RequestId, String> cancelled = new ConcurrentHashMap<>();
    private final RateLimiter limiter;

    private final ProgressNotificationJsonCodec NOTIFICATION_CODEC = new ProgressNotificationJsonCodec();

    public ProgressManager(RateLimiter limiter) {
        if (limiter == null) {
            throw new IllegalArgumentException("limiter required");
        }
        this.limiter = limiter;
    }

    public Optional<ProgressToken> register(RequestId id, JsonObject params) {
        if (!used.add(id) || !active.add(id)) {
            throw new DuplicateRequestException(id);
        }
        if (params != null && params.containsKey("progressToken")) {
            throw new IllegalArgumentException("progressToken must be in _meta");
        }
        var token = ProgressToken.fromMeta(params);
        token.ifPresent(t -> {
            var prev = progress.putIfAbsent(t, Double.NEGATIVE_INFINITY);
            if (prev != null) {
                throw new IllegalArgumentException("Duplicate token: " + t);
            }
            tokens.put(id, t);
        });
        return token;
    }

    public void release(RequestId id) {
        active.remove(id);
        cancelled.remove(id);
        var t = tokens.remove(id);
        if (t != null) {
            progress.remove(t);
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

    public void record(ProgressNotification note) {
        update(note);
        if (note.progress() >= 1.0) {
            progress.remove(note.token());
            tokens.values().removeIf(t -> t.equals(note.token()));
        }
    }

    private void update(ProgressNotification note) {
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

    private boolean isActive(ProgressToken token) {
        return progress.containsKey(token);
    }

    public boolean hasProgress(ProgressToken token) {
        var p = progress.get(token);
        return p != null && p > Double.NEGATIVE_INFINITY;
    }

    public void send(ProgressNotification note, NotificationSender sender) throws IOException {
        if (!isActive(note.token())) {
            return;
        }
        try {
            limiter.requireAllowance(note.token().asString());
            update(note);
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ignore) {
            return;
        }
        sender.send(NotificationMethod.PROGRESS, NOTIFICATION_CODEC.toJson(note));
    }
}
