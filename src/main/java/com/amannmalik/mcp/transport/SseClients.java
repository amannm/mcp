package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.JsonRpcMessageJsonCodec;
import com.amannmalik.mcp.core.MessageRouter;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.util.CloseUtil;
import com.amannmalik.mcp.util.PlatformLog;
import jakarta.json.JsonObject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;

import java.io.IOException;
import java.lang.System.Logger;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

final class SseClients {
    static final JsonCodec<JsonRpcMessage> CODEC = new JsonRpcMessageJsonCodec();
    private static final Logger LOG = PlatformLog.get(SseClients.class);

    @FunctionalInterface
    interface ClientFactory {
        SseClient create(AsyncContext context) throws IOException;
    }

    private final Set<SseClient> general = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<RequestId, SseClient> request = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SseClient> byPrefix = new ConcurrentHashMap<>();
    private final AtomicReference<SseClient> lastGeneral = new AtomicReference<>();
    private final ConcurrentHashMap<RequestId, BlockingQueue<JsonObject>> responses = new ConcurrentHashMap<>();
    private final MessageRouter.Routes routerBindings = new RoutesView();

    SseClient registerGeneral(AsyncContext context, String lastEventId, ClientFactory factory) throws IOException {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(factory, "factory");

        var client = resumeOrCreate(lastEventId, context, factory);
        return registerClient(client, context,
                () -> registerGeneral(client),
                () -> removeGeneral(client));
    }

    SseClient registerRequest(RequestId key, AsyncContext context, ClientFactory factory) throws IOException {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(factory, "factory");

        var client = factory.create(context);
        return registerClient(client, context,
                () -> registerRequest(key, client),
                () -> removeRequest(key, client));
    }

    BlockingQueue<JsonObject> registerResponseQueue(RequestId key, int capacity) {
        Objects.requireNonNull(key, "key");
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>(capacity);
        var existing = responses.putIfAbsent(key, queue);
        if (existing != null) {
            throw new IllegalStateException("duplicate response queue: " + key);
        }
        return queue;
    }

    void removeResponseQueue(RequestId key) {
        removeResponseQueueInternal(key);
    }

    private BlockingQueue<JsonObject> removeResponseQueueInternal(RequestId key) {
        Objects.requireNonNull(key, "key");
        return responses.remove(key);
    }

    private Optional<BlockingQueue<JsonObject>> takeResponseQueue(RequestId key) {
        return Optional.ofNullable(removeResponseQueueInternal(key));
    }

    void removeRequest(RequestId key, SseClient client) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(client, "client");
        request.remove(key, client);
        byPrefix.remove(client.prefix());
        CloseUtil.close(client);
    }

    private SseClient requestClient(RequestId key) {
        Objects.requireNonNull(key, "key");
        return request.get(key);
    }

    private Iterable<SseClient> generalSnapshot() {
        return List.copyOf(general);
    }

    private Optional<SseClient> lastGeneralClient() {
        return Optional.ofNullable(lastGeneral.get());
    }

    void removeGeneral(SseClient client) {
        Objects.requireNonNull(client, "client");
        general.remove(client);
        lastGeneral.set(client);
        CloseUtil.close(client);
    }

    private AsyncListener listener(Runnable cleanup) {
        return new CleanupAsyncListener(cleanup);
    }

    MessageRouter.Routes routes() {
        return routerBindings;
    }

    void failPending() {
        responses.forEach((id, q) -> {
            var err = JsonRpcError.of(id, JsonRpcErrorCode.INTERNAL_ERROR, "Transport closed");
            offerOrThrow(id, q, CODEC.toJson(err));
        });
        responses.clear();
    }

    void clear() {
        general.forEach(SseClient::close);
        general.clear();
        lastGeneral.set(null);
        request.forEach((id, c) -> c.close());
        request.clear();
        byPrefix.clear();
        responses.clear();
    }

    private void offerOrThrow(RequestId id, BlockingQueue<JsonObject> queue, JsonObject message) {
        if (!queue.offer(message)) {
            throw new IllegalStateException("queue full for request " + id);
        }
    }

    private Optional<SseClient> resume(String lastEventId, AsyncContext context) throws IOException {
        var parsed = SseLastEventId.parse(lastEventId);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        var token = parsed.get();
        var candidate = byPrefix.get(token.prefix());
        if (candidate == null) {
            return Optional.empty();
        }
        candidate.attach(context, token.eventId());
        return Optional.of(candidate);
    }

    private SseClient resumeOrCreate(String lastEventId,
                                     AsyncContext context,
                                     ClientFactory factory) throws IOException {
        var resumed = resume(lastEventId, context);
        if (resumed.isPresent()) {
            return resumed.get();
        }
        return factory.create(context);
    }

    private void addCleanupListener(AsyncContext context, Runnable cleanup) {
        context.addListener(listener(cleanup));
    }

    private SseClient registerClient(SseClient client,
                                     AsyncContext context,
                                     Runnable registration,
                                     Runnable cleanup) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(registration, "registration");
        Objects.requireNonNull(cleanup, "cleanup");

        try {
            registration.run();
        } catch (RuntimeException e) {
            CloseUtil.close(client);
            throw e;
        }

        try {
            addCleanupListener(context, cleanup);
        } catch (RuntimeException listenerFailure) {
            try {
                cleanup.run();
            } catch (RuntimeException cleanupFailure) {
                listenerFailure.addSuppressed(cleanupFailure);
            }
            throw listenerFailure;
        }

        return client;
    }

    private void registerGeneral(SseClient client) {
        Objects.requireNonNull(client, "client");
        general.add(client);
        byPrefix.put(client.prefix(), client);
        lastGeneral.set(null);
    }

    private void registerRequest(RequestId key, SseClient client) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(client, "client");
        var existing = request.putIfAbsent(key, client);
        if (existing != null) {
            throw new IllegalStateException("duplicate request client: " + key);
        }
        byPrefix.put(client.prefix(), client);
    }

    private final class RoutesView implements MessageRouter.Routes {
        @Override
        public Optional<SseClient> requestClient(RequestId id) {
            Objects.requireNonNull(id, "id");
            return Optional.ofNullable(SseClients.this.requestClient(id));
        }

        @Override
        public Optional<BlockingQueue<JsonObject>> takeResponseQueue(RequestId id) {
            Objects.requireNonNull(id, "id");
            return SseClients.this.takeResponseQueue(id);
        }

        @Override
        public Iterable<SseClient> generalClients() {
            return SseClients.this.generalSnapshot();
        }

        @Override
        public Optional<SseClient> pendingGeneralClient() {
            return SseClients.this.lastGeneralClient();
        }

        @Override
        public void removeRequestClient(RequestId id, SseClient client) {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(client, "client");
            SseClients.this.removeRequest(id, client);
        }
    }

    private static final class CleanupAsyncListener implements AsyncListener {
        private final Runnable cleanup;

        private CleanupAsyncListener(Runnable cleanup) {
            this.cleanup = Objects.requireNonNull(cleanup, "cleanup");
        }

        @Override
        public void onComplete(AsyncEvent event) {
            cleanup.run();
        }

        @Override
        public void onTimeout(AsyncEvent event) {
            cleanup.run();
        }

        @Override
        public void onError(AsyncEvent event) {
            cleanup.run();
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            Objects.requireNonNull(event, "event").getAsyncContext().addListener(this);
        }
    }

    private record SseLastEventId(String prefix, long eventId) {
        private static Optional<SseLastEventId> parse(String header) {
            if (header == null || header.isBlank()) {
                return Optional.empty();
            }
            var idx = header.lastIndexOf('-');
            if (idx <= 0 || idx == header.length() - 1) {
                return invalid(header, null);
            }
            var prefix = header.substring(0, idx);
            try {
                var eventId = Long.parseLong(header.substring(idx + 1));
                if (eventId < 0) {
                    return invalid(header, null);
                }
                return Optional.of(new SseLastEventId(prefix, eventId));
            } catch (NumberFormatException e) {
                return invalid(header, e);
            }
        }

        private static Optional<SseLastEventId> invalid(String header, Exception cause) {
            if (cause == null) {
                LOG.log(Logger.Level.WARNING, "Invalid Last-Event-ID: " + header);
            } else {
                LOG.log(Logger.Level.WARNING, "Invalid Last-Event-ID: " + header, cause);
            }
            return Optional.empty();
        }
    }
}
