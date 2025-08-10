package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.util.CancelledNotification;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

public sealed class JsonRpcEndpoint implements AutoCloseable permits com.amannmalik.mcp.api.McpClient, com.amannmalik.mcp.api.McpServer {
    protected final Transport transport;
    protected final ProgressManager progress;
    protected final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private final Map<String, Function<JsonRpcRequest, JsonRpcMessage>> requests = new HashMap<>();
    private final Map<String, Consumer<JsonRpcNotification>> notifications = new HashMap<>();
    private final AtomicLong counter;

    protected JsonRpcEndpoint(Transport transport, ProgressManager progress, long initialId) {
        if (transport == null || progress == null) throw new IllegalArgumentException("transport and progress required");
        this.transport = transport;
        this.progress = progress;
        this.counter = new AtomicLong(initialId);
    }

    protected RequestId nextId() {
        return new RequestId.NumericId(counter.getAndIncrement());
    }

    public void registerRequest(String method, Function<JsonRpcRequest, JsonRpcMessage> handler) {
        requests.put(method, handler);
    }

    public void registerNotification(String method, Consumer<JsonRpcNotification> handler) {
        notifications.put(method, handler);
    }

    public JsonRpcMessage request(RequestMethod method, JsonObject params) throws IOException {
        return request(method.method(), params, McpConfiguration.current().defaultMs());
    }

    public JsonRpcMessage request(String method, JsonObject params) throws IOException {
        return request(method, params, McpConfiguration.current().defaultMs());
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
            case JsonRpcRequest req -> handleRequest(req, true)
                    .ifPresent(r -> {
                        try {
                            send(r);
                        } catch (IOException ignore) {
                        }
                    });
            case JsonRpcNotification note -> handleNotification(note);
            case JsonRpcResponse resp -> complete(resp.id(), resp);
            case JsonRpcError err -> complete(err.id(), err);
        }
    }

    private void complete(RequestId id, JsonRpcMessage msg) {
        var f = pending.remove(id);
        if (f != null) f.complete(msg);
    }

    private Optional<JsonRpcMessage> handleRequest(JsonRpcRequest req, boolean cancellable) {
        if (req == null) throw new IllegalArgumentException("request required");

        final Optional<ProgressToken> token;
        try {
            token = progress.register(req.id(), req.params());
        } catch (IllegalArgumentException e) {
            progress.release(req.id());
            return Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage()));
        }

        try {
            token.ifPresent(t -> sendProgress(t, 0.0));
            if (cancellable && progress.isCancelled(req.id())) return Optional.empty();
            JsonRpcMessage resp = dispatch(req);
            if (cancellable && progress.isCancelled(req.id())) return Optional.empty();
            token.ifPresent(t -> sendProgress(t, 1.0));
            return Optional.of(resp);
        } finally {
            progress.release(req.id());
        }
    }

    private JsonRpcMessage dispatch(JsonRpcRequest req) {
        try {
            var handler = requests.get(req.method());
            if (handler == null) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Unknown method: " + req.method());
            }
            var resp = handler.apply(req);
            if (resp == null) throw new IllegalStateException("handler returned null");
            return resp;
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private void handleNotification(JsonRpcNotification note) {
        var handler = notifications.get(note.method());
        if (handler != null) handler.accept(note);
    }

    private void sendProgress(ProgressToken token, double current) {
        String msg = current >= 1.0 ? "completed" : "in progress";
        try {
            progress.send(new ProgressNotification(token, current, 1.0, msg), m -> {
                try {
                    send(m);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
        } catch (IOException ignore) {
        }
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }
}

