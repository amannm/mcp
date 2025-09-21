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
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public final class ProgressManager {
    private static final Logger LOG = PlatformLog.get(ProgressManager.class);
    private static final ProgressNotificationJsonCodec NOTIFICATION_CODEC = new ProgressNotificationJsonCodec();
    private final Map<ProgressToken, TokenState> tokensByProgress = new ConcurrentHashMap<>();
    private final Map<RequestId, RequestRegistration> requests = new ConcurrentHashMap<>();
    private final Set<RequestId> used = ConcurrentHashMap.newKeySet();
    private final RateLimiter limiter;

    public ProgressManager(RateLimiter limiter) {
        this.limiter = Objects.requireNonNull(limiter, "limiter");
    }

    private static void ensureProgressTokenPlacement(JsonObject params) {
        if (params != null && params.containsKey("progressToken")) {
            throw new IllegalArgumentException("progressToken must be in _meta");
        }
    }

    public Optional<ProgressToken> register(RequestId id, JsonObject params) {
        Objects.requireNonNull(id, "id");
        ensureProgressTokenPlacement(params);
        var tokenOpt = ProgressToken.fromMeta(params);
        if (!used.add(id)) {
            throw new DuplicateRequestException(id);
        }
        var registration = createRegistration(id, tokenOpt.orElse(null));
        var previous = requests.putIfAbsent(id, registration);
        if (previous != null) {
            used.remove(id);
            throw new DuplicateRequestException(id);
        }
        try {
            tokenOpt.ifPresent(candidate -> bindToken(id, registration, candidate));
        } catch (RuntimeException e) {
            requests.remove(id, registration);
            used.remove(id);
            throw e;
        }
        return tokenOpt;
    }

    public void release(RequestId id) {
        Objects.requireNonNull(id, "id");
        var registration = requests.remove(id);
        if (registration == null) {
            return;
        }
        registration.token().ifPresent(token ->
                registration.tokenState().ifPresent(state ->
                        tokensByProgress.computeIfPresent(token, (ignored, current) -> {
                            if (!current.removeRequest(id)) {
                                return current;
                            }
                            current.clearRequests();
                            current.deactivate();
                            return null;
                        })));
    }

    public Optional<String> cancel(RequestId id, String reason) {
        Objects.requireNonNull(id, "id");
        var registration = requests.get(id);
        if (registration == null) {
            return Optional.empty();
        }
        registration.cancel(reason);
        return Optional.ofNullable(reason);
    }

    public boolean isCancelled(RequestId id) {
        Objects.requireNonNull(id, "id");
        var registration = requests.get(id);
        return registration != null && registration.isCancelled();
    }

    public String reason(RequestId id) {
        Objects.requireNonNull(id, "id");
        var registration = requests.get(id);
        if (registration == null) {
            return null;
        }
        return registration.cancellationReason().orElse(null);
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
            requests.computeIfPresent(requestId, (ignored, registration) -> {
                registration.clearTokenState();
                return registration;
            });
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
            limiter.requireAllowance(note.token().toString());
            tokenState.advance(note.progress());
        } catch (IllegalArgumentException | IllegalStateException | SecurityException e) {
            LOG.log(Logger.Level.WARNING, "Progress update rejected", e);
            return;
        }
        sender.send(NotificationMethod.PROGRESS, NOTIFICATION_CODEC.toJson(note));
    }

    private RequestRegistration createRegistration(RequestId id, ProgressToken token) {
        Objects.requireNonNull(id, "id");
        if (token == null) {
            return RequestRegistration.withoutToken();
        }
        var state = new TokenState();
        state.addRequest(id);
        return RequestRegistration.withToken(token, state);
    }

    private void bindToken(RequestId id, RequestRegistration registration, ProgressToken token) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(registration, "registration");
        Objects.requireNonNull(token, "token");
        var state = registration.tokenState()
                .orElseThrow(() -> new IllegalStateException("Missing token state for " + id));
        var existing = tokensByProgress.putIfAbsent(token, state);
        if (existing != null) {
            throw new IllegalArgumentException("Duplicate token: " + token);
        }
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

    private static final class RequestRegistration {
        private final ProgressToken token;
        private final AtomicReference<TokenState> tokenState;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<String> cancellationReason = new AtomicReference<>();

        private RequestRegistration(ProgressToken token, TokenState state) {
            this.token = token;
            this.tokenState = new AtomicReference<>(state);
        }

        static RequestRegistration withoutToken() {
            return new RequestRegistration(null, null);
        }

        static RequestRegistration withToken(ProgressToken token, TokenState state) {
            return new RequestRegistration(Objects.requireNonNull(token, "token"), Objects.requireNonNull(state, "state"));
        }

        Optional<ProgressToken> token() {
            return Optional.ofNullable(token);
        }

        Optional<TokenState> tokenState() {
            return Optional.ofNullable(tokenState.get());
        }

        void clearTokenState() {
            tokenState.set(null);
        }

        void cancel(String reason) {
            cancelled.set(true);
            cancellationReason.set(reason);
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        Optional<String> cancellationReason() {
            return Optional.ofNullable(cancellationReason.get());
        }
    }
}
