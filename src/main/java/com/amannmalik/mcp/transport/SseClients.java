package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.JsonRpcMessageJsonCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.util.CloseUtil;
import jakarta.json.JsonObject;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;

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
    private final Set<SseClient> general = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<RequestId, SseClient> request = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SseClient> byPrefix = new ConcurrentHashMap<>();
    private final AtomicReference<SseClient> lastGeneral = new AtomicReference<>();
    private final ConcurrentHashMap<RequestId, BlockingQueue<JsonObject>> responses = new ConcurrentHashMap<>();

    Optional<SseClient> findByPrefix(String prefix) {
        Objects.requireNonNull(prefix, "prefix");
        return Optional.ofNullable(byPrefix.get(prefix));
    }

    void registerGeneral(SseClient client) {
        Objects.requireNonNull(client, "client");
        general.add(client);
        byPrefix.put(client.prefix(), client);
        lastGeneral.set(null);
    }

    void registerRequest(RequestId key, SseClient client) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(client, "client");
        var existing = request.putIfAbsent(key, client);
        if (existing != null) {
            throw new IllegalStateException("duplicate request client: " + key);
        }
        byPrefix.put(client.prefix(), client);
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
        Objects.requireNonNull(key, "key");
        responses.remove(key);
    }

    void removeRequest(RequestId key, SseClient client) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(client, "client");
        request.remove(key, client);
        byPrefix.remove(client.prefix());
        CloseUtil.close(client);
    }

    AsyncListener requestListener(RequestId key, SseClient client) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(client, "client");
        return listener(() -> removeRequest(key, client));
    }

    SseClient requestClient(RequestId key) {
        Objects.requireNonNull(key, "key");
        return request.get(key);
    }

    BlockingQueue<JsonObject> removeResponse(RequestId key) {
        Objects.requireNonNull(key, "key");
        return responses.remove(key);
    }

    Iterable<SseClient> generalSnapshot() {
        return List.copyOf(general);
    }

    SseClient lastGeneralClient() {
        return lastGeneral.get();
    }

    void removeGeneral(SseClient client) {
        Objects.requireNonNull(client, "client");
        general.remove(client);
        lastGeneral.set(client);
        CloseUtil.close(client);
    }

    AsyncListener generalListener(SseClient client) {
        Objects.requireNonNull(client, "client");
        return listener(() -> removeGeneral(client));
    }

    private AsyncListener listener(Runnable cleanup) {
        return new CleanupAsyncListener(cleanup);
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
        }
    }
}

