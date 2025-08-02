package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.RequestId;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class JsonRpcCallManager {
    public interface Sender { void send(JsonRpcMessage msg) throws IOException; }
    public interface TimeoutHandler { void onTimeout(RequestId id) throws IOException; }

    private final Sender sender;
    private final TimeoutHandler timeoutHandler;
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();

    public JsonRpcCallManager(Sender sender, TimeoutHandler timeoutHandler) {
        if (sender == null) throw new IllegalArgumentException("sender required");
        if (timeoutHandler == null) throw new IllegalArgumentException("timeout handler required");
        this.sender = sender;
        this.timeoutHandler = timeoutHandler;
    }

    public JsonRpcMessage call(RequestId id, String method, jakarta.json.JsonObject params, long timeoutMillis) throws IOException {
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(id, future);
        sender.send(new JsonRpcRequest(id, method, params));
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        } catch (TimeoutException e) {
            timeoutHandler.onTimeout(id);
            throw new IOException("Request timed out after " + timeoutMillis + " ms");
        } finally {
            pending.remove(id);
        }
    }

    public void complete(JsonRpcMessage msg) {
        switch (msg) {
            case JsonRpcResponse resp -> {
                CompletableFuture<JsonRpcMessage> f = pending.remove(resp.id());
                if (f != null) f.complete(resp);
            }
            case JsonRpcError err -> {
                CompletableFuture<JsonRpcMessage> f = pending.remove(err.id());
                if (f != null) f.complete(err);
            }
            default -> {}
        }
    }

    public void failAll(IOException e) {
        pending.values().forEach(f -> f.completeExceptionally(e));
        pending.clear();
    }
}
