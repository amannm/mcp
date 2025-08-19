package com.amannmalik.mcp.jsonrpc;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.codec.CancelledNotificationJsonCodec;
import com.amannmalik.mcp.codec.JsonRpcMessageJsonCodec;
import com.amannmalik.mcp.core.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

public sealed class JsonRpcEndpoint implements AutoCloseable permits McpClient, McpServer {
    public static final JsonRpcMessageJsonCodec CODEC = new JsonRpcMessageJsonCodec();
    protected static final CancelledNotificationJsonCodec CANCEL_CODEC = new CancelledNotificationJsonCodec();
    public final Transport transport;
    protected final ProgressManager progress;
    public final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
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

    protected JsonRpcMessage await(RequestId id, CompletableFuture<JsonRpcMessage> future, Duration timeoutMillis) throws IOException {
        try {
            return future.get(timeoutMillis.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            cancelTimeout(id);
            throw new IOException(McpServerConfiguration.defaultConfiguration().errorTimeout() + " after " + timeoutMillis + " ms", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw unwrapExecutionException(e);
        }
    }

    protected final JsonRpcMessage awaitAndProcess(
            RequestId id,
            CompletableFuture<JsonRpcMessage> future,
            Duration timeout,
            Supplier<Optional<JsonObject>> receiver,
            Consumer<IllegalArgumentException> invalidHandler,
            String timeoutMessage) throws IOException {
        var end = System.currentTimeMillis() + timeout.toMillis();
        try {
            while (!future.isDone()) {
                if (System.currentTimeMillis() >= end) {
                    cancelTimeout(id);
                    throw new IOException(timeoutMessage + " after " + timeout.toMillis() + " ms");
                }
                var obj = receiver.get();
                if (obj.isPresent()) {
                    try {
                        process(CODEC.fromJson(obj.get()));
                    } catch (IllegalArgumentException e) {
                        invalidHandler.accept(e);
                    }
                }
            }
            return getCompleted(future);
        } finally {
            pending.remove(id);
        }
    }

    private static IOException unwrapExecutionException(ExecutionException e) {
        var cause = e.getCause();
        if (cause instanceof IOException io) return io;
        return new IOException(cause);
    }

    private JsonRpcMessage getCompleted(CompletableFuture<JsonRpcMessage> future) throws IOException {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw unwrapExecutionException(e);
        }
    }

    private void cancelTimeout(RequestId id) {
        try {
            var params = CANCEL_CODEC.toJson(new CancelledNotification(id, "timeout"));
            send(new JsonRpcNotification(NotificationMethod.CANCELLED.method(), params));
        } catch (IOException ignore) {
        }
    }

    protected final synchronized void send(JsonRpcMessage msg) throws IOException {
        transport.send(CODEC.toJson(msg));
    }

    public final void process(JsonRpcMessage msg) throws IOException {
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
        } catch (DuplicateRequestException e) {
            progress.release(req.id());
            return Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_REQUEST, e.getMessage()));
        } catch (IllegalArgumentException e) {
            progress.release(req.id());
            return Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage()));
        }

        try {
            token.ifPresent(t -> sendProgress(t, 0.0));
            if (cancellable && progress.isCancelled(req.id())) return Optional.empty();
            var resp = dispatch(req);
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
        var msg = current >= 1.0 ? "completed" : "in progress";
        try {
            progress.send(new ProgressNotification(token, current, 1.0, msg), (m, payload) -> {
                try {
                    send(new JsonRpcNotification(m.method(), payload));
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

