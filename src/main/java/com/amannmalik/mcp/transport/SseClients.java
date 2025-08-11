package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.RequestId;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.JsonRpcMessageJsonCodec;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.util.CloseUtil;
import jakarta.json.JsonObject;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

final class SseClients {
    static final JsonCodec<JsonRpcMessage> CODEC = new JsonRpcMessageJsonCodec();
    final Set<SseClient> general = ConcurrentHashMap.newKeySet();
    final ConcurrentHashMap<String, SseClient> request = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, SseClient> byPrefix = new ConcurrentHashMap<>();
    final AtomicReference<SseClient> lastGeneral = new AtomicReference<>();
    final ConcurrentHashMap<String, BlockingQueue<JsonObject>> responses = new ConcurrentHashMap<>();

    void removeRequest(String key, SseClient client) {
        request.remove(key);
        byPrefix.remove(client.prefix);
        CloseUtil.closeQuietly(client);
    }

    AsyncListener requestListener(String key, SseClient client) {
        return new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                removeRequest(key, client);
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                removeRequest(key, client);
            }

            @Override
            public void onError(AsyncEvent event) {
                removeRequest(key, client);
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
            }
        };
    }

    void removeGeneral(SseClient client) {
        general.remove(client);
        lastGeneral.set(client);
        CloseUtil.closeQuietly(client);
    }

    AsyncListener generalListener(SseClient client) {
        return new AsyncListener() {
            @Override
            public void onComplete(AsyncEvent event) {
                removeGeneral(client);
            }

            @Override
            public void onTimeout(AsyncEvent event) {
                removeGeneral(client);
            }

            @Override
            public void onError(AsyncEvent event) {
                removeGeneral(client);
            }

            @Override
            public void onStartAsync(AsyncEvent event) {
            }
        };
    }

    void failPending() {
        responses.forEach((id, q) -> {
            RequestId reqId = RequestId.parse(id);
            JsonRpcError err = JsonRpcError.of(reqId, JsonRpcErrorCode.INTERNAL_ERROR, "Transport closed");
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
    }
}

