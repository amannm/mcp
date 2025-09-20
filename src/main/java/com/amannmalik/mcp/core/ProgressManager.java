package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.NotificationMethod;
import com.amannmalik.mcp.api.ProgressNotification;
import com.amannmalik.mcp.api.ProgressToken;
import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.codec.ProgressNotificationJsonCodec;
import com.amannmalik.mcp.util.NotificationSender;
import com.amannmalik.mcp.util.PlatformLog;
import com.amannmalik.mcp.util.RateLimiter;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.lang.System.Logger;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
        this.limiter = Objects.requireNonNull(limiter, "limiter");
    }

    public Optional<ProgressToken> register(RequestId id, JsonObject params) {
        Objects.requireNonNull(id, "id");
        ensureNewRequest(id);
        ensureProgressTokenPlacement(params);
        var token = ProgressToken.fromMeta(params);
        token.ifPresent(candidate -> associate(id, candidate));
        return token;
    }

    private void ensureNewRequest(RequestId id) {
        if (!used.add(id)) {
            throw new DuplicateRequestException(id);
        }
        if (!active.add(id)) {
            used.remove(id);
            throw new DuplicateRequestException(id);
        }
    }

    private static void ensureProgressTokenPlacement(JsonObject params) {
        if (params != null && params.containsKey("progressToken")) {
            throw new IllegalArgumentException("progressToken must be in _meta");
        }
    }

    public void release(RequestId id) {
        Objects.requireNonNull(id, "id");
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
        Objects.requireNonNull(id, "id");
        if (active.contains(id)) {
            cancelled.put(id, reason);
        }
    }

    public boolean isCancelled(RequestId id) {
        Objects.requireNonNull(id, "id");
        return cancelled.containsKey(id);
    }

    public String reason(RequestId id) {
        Objects.requireNonNull(id, "id");
        return cancelled.get(id);
    }

    public void record(ProgressNotification note) {
        Objects.requireNonNull(note, "note");
        var state = requireActiveState(note.token());
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
        Objects.requireNonNull(token, "token");
        return findActiveState(token)
                .map(TokenState::hasProgress)
                .orElse(false);
    }

    public void send(ProgressNotification note, NotificationSender sender) throws IOException {
        Objects.requireNonNull(note, "note");
        Objects.requireNonNull(sender, "sender");
        var state = findActiveState(note.token());
        if (state.isEmpty()) {
            return;
        }
        var tokenState = state.get();
        try {
            limiter.requireAllowance(note.token().asString());
            tokenState.advance(note.progress());
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            LOG.log(Logger.Level.WARNING, "Progress update rejected", e);
            return;
        }
        sender.send(NotificationMethod.PROGRESS, NOTIFICATION_CODEC.toJson(note));
    }

    private void associate(RequestId id, ProgressToken token) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(token, "token");
        var state = new TokenState();
        state.addRequest(id);
        var existing = tokensByProgress.putIfAbsent(token, state);
        if (existing != null) {
            throw new IllegalArgumentException("Duplicate token: " + token);
        }
        tokens.put(id, token);
    }

    private TokenState requireActiveState(ProgressToken token) {
        return findActiveState(token)
                .orElseThrow(() -> new IllegalStateException("Unknown progress token: " + token));
    }

    private Optional<TokenState> findActiveState(ProgressToken token) {
        Objects.requireNonNull(token, "token");
        var state = tokensByProgress.get(token);
        if (state == null || !state.isActive()) {
            return Optional.empty();
        }
        return Optional.of(state);
    }

    private static final class TokenState {
        private final Set<RequestId> requestIds = ConcurrentHashMap.newKeySet();
        private final ReentrantLock lock = new ReentrantLock();
        private volatile boolean active = true;
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
            active = false;
        }

        boolean isActive() {
            return active;
        }

        void advance(double value) {
            lock.lock();
            try {
                if (!active) {
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
