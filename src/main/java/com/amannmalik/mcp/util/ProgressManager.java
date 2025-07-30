package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.security.RateLimiter;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ProgressManager {
    private final ProgressTracker tracker = new ProgressTracker();
    private final Map<RequestId, ProgressToken> tokens = new ConcurrentHashMap<>();
    private final RateLimiter limiter;

    public ProgressManager(RateLimiter limiter) {
        if (limiter == null) throw new IllegalArgumentException("limiter required");
        this.limiter = limiter;
    }

    public Optional<ProgressToken> register(RequestId id, JsonObject params) {
        Optional<ProgressToken> token = ProgressUtil.tokenFromMeta(params);
        token.ifPresent(t -> {
            tracker.register(t);
            tokens.put(id, t);
        });
        return token;
    }

    public void release(RequestId id) {
        ProgressToken t = tokens.remove(id);
        if (t != null) tracker.release(t);
    }

    public void record(ProgressNotification note) {
        tracker.update(note);
        if (note.progress() >= 1.0) {
            tracker.release(note.token());
            tokens.values().removeIf(t -> t.equals(note.token()));
        }
    }

    public boolean hasProgress(ProgressToken token) {
        return tracker.hasProgress(token);
    }

    public void send(ProgressNotification note, NotificationSender sender) throws IOException {
        ProgressUtil.sendProgress(note, tracker, limiter, sender);
    }
}
