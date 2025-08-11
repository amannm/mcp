package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.CancelledNotificationJsonCodec;
import com.amannmalik.mcp.codec.JsonRpcMessageJsonCodec;
import com.amannmalik.mcp.core.ProgressManager;
import com.amannmalik.mcp.jsonrpc.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;

sealed class JsonRpcEndpoint implements AutoCloseable permits McpClient, McpServer {
    protected static final JsonRpcMessageJsonCodec CODEC = new JsonRpcMessageJsonCodec();
    protected static final CancelledNotificationJsonCodec CANCEL_CODEC = new CancelledNotificationJsonCodec();
    protected final Transport transport;
    protected final ProgressManager progress;
    protected final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private final Map<RequestMethod, Function<JsonRpcRequest, JsonRpcMessage>> requests = new EnumMap<>(RequestMethod.class);
    private final Map<NotificationMethod, Consumer<JsonRpcNotification>> notifications = new EnumMap<>(NotificationMethod.class);
    private final AtomicLong counter;

    protected JsonRpcEndpoint(Transport transport, ProgressManager progress, long initialId) {
        if (transport == null || progress == null) throw new IllegalArgumentException("transport and progress required");
        this.transport = transport;
        this.progress = progress;
        this.counter = new AtomicLong(initialId);
    }

    protected final RequestId nextId() {
        return new RequestId.NumericId(counter.getAndIncrement());
    }

    public final void registerRequest(RequestMethod method, Function<JsonRpcRequest, JsonRpcMessage> handler) {
        requests.put(method, handler);
    }

    protected final void registerNotification(NotificationMethod method, Consumer<JsonRpcNotification> handler) {
        notifications.put(method, handler);
    }

    protected JsonRpcMessage await(RequestId id, CompletableFuture<JsonRpcMessage> future, long timeoutMillis) throws IOException {
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
                notify(NotificationMethod.CANCELLED,
                        CANCEL_CODEC.toJson(new CancelledNotification(id, "timeout")));
            } catch (IOException ignore) {
            }
            throw new IOException(McpServerConfiguration.defaultConfiguration().errorTimeout() + " after " + timeoutMillis + " ms", e);
        }
    }

    protected final void notify(NotificationMethod method, JsonObject params) throws IOException {
        send(new JsonRpcNotification(method.method(), params));
    }

    protected final synchronized void send(JsonRpcMessage msg) throws IOException {
        transport.send(CODEC.toJson(msg));
    }

    protected final void process(JsonRpcMessage msg) throws IOException {
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
            var method = RequestMethod.from(req.method())
                    .flatMap(m -> Optional.ofNullable(requests.get(m))
                            .map(f -> Map.entry(m, f)));
            if (method.isEmpty()) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Unknown method: " + req.method());
            }
            var resp = method.get().getValue().apply(req);
            if (resp == null) throw new IllegalStateException("handler returned null");
            return resp;
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private void handleNotification(JsonRpcNotification note) {
        NotificationMethod.from(note.method())
                .map(notifications::get)
                .ifPresent(h -> h.accept(note));
    }

    private void sendProgress(ProgressToken token, double current) {
        String msg = current >= 1.0 ? "completed" : "in progress";
        try {
            progress.send(new ProgressNotification(token, current, 1.0, msg), (m, payload) -> {
                try {
                    notify(m, payload);
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

