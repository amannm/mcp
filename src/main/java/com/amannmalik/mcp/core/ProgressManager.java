package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.codec.ProgressNotificationJsonCodec;
import com.amannmalik.mcp.util.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.lang.System.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public final class ProgressManager {
    private static final Logger LOG = PlatformLog.get(ProgressManager.class);
    private final Map<ProgressToken, TokenState> tokensByProgress = new ConcurrentHashMap<>();
    private final Map<RequestId, ProgressToken> tokens = new ConcurrentHashMap<>();
    private final Set<RequestId> active = ConcurrentHashMap.newKeySet();
    private final Set<RequestId> used = ConcurrentHashMap.newKeySet();
    private final Map<RequestId, String> cancelled = new ConcurrentHashMap<>();
    private final RateLimiter limiter;
    private static final ProgressNotificationJsonCodec NOTIFICATION_CODEC = new ProgressNotificationJsonCodec();

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
        token.ifPresent(t -> associate(id, t));
        return token;
    }

    public void release(RequestId id) {
        active.remove(id);
        cancelled.remove(id);
        var token = tokens.remove(id);
        if (token == null) {
            return;
        }
        tokensByProgress.computeIfPresent(token, (ignored, state) -> {
            if (!state.removeRequest(id)) {
                return state;
            }
            state.clearRequests();
            state.deactivate();
            return null;
        });
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
        var state = requireState(note.token());
        state.advance(note.progress());
        if (note.progress() >= 1.0) {
            completeToken(note.token(), state);
        }
    }

    private void completeToken(ProgressToken token, TokenState state) {
        if (!tokensByProgress.remove(token, state)) {
            return;
        }
        state.deactivate();
        for (var requestId : state.requestIdsSnapshot()) {
            tokens.remove(requestId, token);
        }
        state.clearRequests();
    }

    public boolean hasProgress(ProgressToken token) {
        var state = tokensByProgress.get(token);
        return state != null && state.hasProgress();
    }

    public void send(ProgressNotification note, NotificationSender sender) throws IOException {
        var state = tokensByProgress.get(note.token());
        if (state == null || !state.isActive()) {
            return;
        }
        try {
            limiter.requireAllowance(note.token().asString());
            state.advance(note.progress());
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            LOG.log(Logger.Level.WARNING, "Progress update rejected", e);
            return;
        }
        sender.send(NotificationMethod.PROGRESS, NOTIFICATION_CODEC.toJson(note));
    }

    private void associate(RequestId id, ProgressToken token) {
        var state = new TokenState();
        state.addRequest(id);
        var existing = tokensByProgress.putIfAbsent(token, state);
        if (existing != null) {
            throw new IllegalArgumentException("Duplicate token: " + token);
        }
        tokens.put(id, token);
    }

    private TokenState requireState(ProgressToken token) {
        var state = tokensByProgress.get(token);
        if (state == null || !state.isActive()) {
            throw new IllegalStateException("Unknown progress token: " + token);
        }
        return state;
    }

    private static final class TokenState {
        private final Set<RequestId> requestIds = ConcurrentHashMap.newKeySet();
        private final ReentrantLock lock = new ReentrantLock();
        private final AtomicBoolean active = new AtomicBoolean(true);
        private volatile boolean hasProgress;
        private volatile double progress;

        void addRequest(RequestId id) {
            requestIds.add(id);
        }

        boolean removeRequest(RequestId id) {
            requestIds.remove(id);
            return requestIds.isEmpty();
        }

        void deactivate() {
            active.set(false);
        }

        boolean isActive() {
            return active.get();
        }

        void advance(double value) {
            lock.lock();
            try {
                if (!active.get()) {
                    throw new IllegalStateException("Progress token no longer active");
                }
                if (hasProgress && value <= progress) {
                    throw new IllegalArgumentException("progress must increase");
                }
                progress = value;
                hasProgress = true;
            } finally {
                lock.unlock();
            }
        }

        boolean hasProgress() {
            return hasProgress;
        }

        List<RequestId> requestIdsSnapshot() {
            return List.copyOf(requestIds);
        }

        void clearRequests() {
            requestIds.clear();
        }
    }
}
