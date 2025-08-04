package com.amannmalik.mcp.core;

import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class JsonRpcEndpoint implements AutoCloseable {
    protected final Transport transport;
    protected final ProgressManager progress;
    protected final JsonRpcRequestProcessor processor;
    protected final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private final AtomicLong counter;

    protected JsonRpcEndpoint(Transport transport, ProgressManager progress, long initialId) {
        if (transport == null || progress == null) throw new IllegalArgumentException("transport and progress required");
        this.transport = transport;
        this.progress = progress;
        this.processor = new JsonRpcRequestProcessor(progress, m -> {
            try {
                send(m);
            } catch (IOException ignore) {
            }
        });
        this.counter = new AtomicLong(initialId);
    }

    protected RequestId nextId() {
        return new RequestId.NumericId(counter.getAndIncrement());
    }

    public JsonRpcMessage request(RequestMethod method, JsonObject params) throws IOException {
        return request(method.method(), params, Timeouts.DEFAULT_TIMEOUT_MS);
    }

    public JsonRpcMessage request(String method, JsonObject params) throws IOException {
        return request(method, params, Timeouts.DEFAULT_TIMEOUT_MS);
    }

    public JsonRpcMessage request(RequestMethod method, JsonObject params, long timeoutMillis) throws IOException {
        return request(method.method(), params, timeoutMillis);
    }

    public JsonRpcMessage request(String method, JsonObject params, long timeoutMillis) throws IOException {
        return doRequest(method, params, timeoutMillis);
    }

    protected JsonRpcMessage doRequest(String method, JsonObject params, long timeoutMillis) throws IOException {
        RequestId id = nextId();
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(id, future);
        progress.register(id, params);
        send(new JsonRpcRequest(id, method, params));
        try {
            return await(id, future, timeoutMillis);
        } finally {
            pending.remove(id);
            progress.release(id);
        }
    }

    private JsonRpcMessage await(RequestId id, CompletableFuture<JsonRpcMessage> future, long timeoutMillis) throws IOException {
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        } catch (TimeoutException e) {
            try {
                notify(NotificationMethod.CANCELLED.method(),
                        CancelledNotification.CODEC.toJson(new CancelledNotification(id, "timeout")));
            } catch (IOException ignore) {
            }
            throw new IOException(McpConfiguration.current().errorTimeout() + " after " + timeoutMillis + " ms", e);
        }
    }

    protected void notify(String method, JsonObject params) throws IOException {
        send(new JsonRpcNotification(method, params));
    }

    protected synchronized void send(JsonRpcMessage msg) throws IOException {
        transport.send(JsonRpcCodec.CODEC.toJson(msg));
    }

    protected void process(JsonRpcMessage msg) throws IOException {
        switch (msg) {
            case JsonRpcRequest req -> processor.handle(req, true)
                    .ifPresent(r -> {
                        try {
                            send(r);
                        } catch (IOException ignore) {
                        }
                    });
            case JsonRpcNotification note -> processor.handle(note);
            case JsonRpcResponse resp -> complete(resp.id(), resp);
            case JsonRpcError err -> complete(err.id(), err);
            default -> {
            }
        }
    }

    private void complete(RequestId id, JsonRpcMessage msg) {
        var f = pending.remove(id);
        if (f != null) f.complete(msg);
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }
}

