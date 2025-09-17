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
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

final class SseClients {
    static final JsonCodec<JsonRpcMessage> CODEC = new JsonRpcMessageJsonCodec();
    final Set<SseClient> general = ConcurrentHashMap.newKeySet();
    final ConcurrentHashMap<RequestId, SseClient> request = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, SseClient> byPrefix = new ConcurrentHashMap<>();
    final AtomicReference<SseClient> lastGeneral = new AtomicReference<>();
    final ConcurrentHashMap<RequestId, BlockingQueue<JsonObject>> responses = new ConcurrentHashMap<>();

    void removeRequest(RequestId key, SseClient client) {
        request.remove(key, client);
        byPrefix.remove(client.prefix());
        CloseUtil.close(client);
    }

    AsyncListener requestListener(RequestId key, SseClient client) {
        return listener(() -> removeRequest(key, client));
    }

    SseClient requestClient(RequestId key) {
        return request.get(key);
    }

    BlockingQueue<JsonObject> removeResponse(RequestId key) {
        return responses.remove(key);
    }

    Iterable<SseClient> generalSnapshot() {
        return List.copyOf(general);
    }

    SseClient lastGeneralClient() {
        return lastGeneral.get();
    }

    void removeGeneral(SseClient client) {
        general.remove(client);
        lastGeneral.set(client);
        CloseUtil.close(client);
    }

    AsyncListener generalListener(SseClient client) {
        return listener(() -> removeGeneral(client));
    }

    private AsyncListener listener(Runnable cleanup) {
        return new AsyncListener() {
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
        };
    }

    void failPending() {
        responses.forEach((id, q) -> {
            var err = JsonRpcError.of(id, JsonRpcErrorCode.INTERNAL_ERROR, "Transport closed");
            if (!q.offer(CODEC.toJson(err))) {
                throw new IllegalStateException("queue full");
            }
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
}

