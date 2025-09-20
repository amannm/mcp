package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.CancelledNotificationJsonCodec;
import com.amannmalik.mcp.codec.JsonRpcMessageJsonCodec;
import com.amannmalik.mcp.core.DuplicateRequestException;
import com.amannmalik.mcp.core.ProgressManager;
import com.amannmalik.mcp.jsonrpc.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;

sealed class JsonRpcEndpoint implements AutoCloseable permits McpClient, McpServer {
    public static final JsonRpcMessageJsonCodec CODEC = new JsonRpcMessageJsonCodec();
    protected static final CancelledNotificationJsonCodec CANCEL_CODEC = new CancelledNotificationJsonCodec();
    public final Transport transport;
    public final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    protected final ProgressManager progress;
    private final Map<RequestMethod, Function<JsonRpcRequest, JsonRpcMessage>> requests = new EnumMap<>(RequestMethod.class);
    private final Map<NotificationMethod, Consumer<JsonRpcNotification>> notifications = new EnumMap<>(NotificationMethod.class);
    private final AtomicLong counter;

    protected JsonRpcEndpoint(Transport transport, ProgressManager progress, long initialId) {
        this.transport = Objects.requireNonNull(transport, "transport required");
        this.progress = Objects.requireNonNull(progress, "progress required");
        this.counter = new AtomicLong(initialId);
    }

    private static IOException unwrapExecutionException(ExecutionException e) {
        var cause = e.getCause();
        if (cause instanceof IOException io) {
            return io;
        }
        return new IOException(cause);
    }

    protected final RequestId nextId() {
        return new RequestId.NumericId(counter.getAndIncrement());
    }

    public final void registerRequest(RequestMethod method, Function<JsonRpcRequest, JsonRpcMessage> handler) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(handler, "handler");
        var previous = requests.putIfAbsent(method, handler);
        if (previous != null) {
            throw new IllegalStateException("Request handler already registered: " + method);
        }
    }

    protected final void registerNotification(NotificationMethod method, Consumer<JsonRpcNotification> handler) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(handler, "handler");
        var previous = notifications.putIfAbsent(method, handler);
        if (previous != null) {
            throw new IllegalStateException("Notification handler already registered: " + method);
        }
    }

    protected JsonRpcMessage await(RequestId id, CompletableFuture<JsonRpcMessage> future, Duration timeout) throws IOException {
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            cancelTimeout(id);
            throw new IOException("Request timed out after " + timeout.toMillis() + " ms", e);
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected final synchronized void send(JsonRpcMessage msg) throws IOException {
        transport.send(CODEC.toJson(msg));
    }

    public final void process(JsonRpcMessage msg) throws IOException {
        switch (msg) {
            case JsonRpcRequest req -> {
                var resp = handleRequest(req, true);
                if (resp.isPresent()) {
                    send(resp.get());
                }
            }
            case JsonRpcNotification note -> handleNotification(note);
            case JsonRpcResponse resp -> complete(resp.id(), resp);
            case JsonRpcError err -> complete(err.id(), err);
        }
    }

    private void complete(RequestId id, JsonRpcMessage msg) {
        var f = pending.remove(id);
        if (f != null) {
            f.complete(msg);
        }
    }

    private Optional<JsonRpcMessage> handleRequest(JsonRpcRequest req, boolean cancellable) {
        if (req == null) {
            throw new IllegalArgumentException("request required");
        }

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
            if (cancellable && progress.isCancelled(req.id())) {
                return Optional.empty();
            }
            var resp = dispatch(req);
            if (cancellable && progress.isCancelled(req.id())) {
                return Optional.empty();
            }
            token.ifPresent(t -> sendProgress(t, 1.0));
            return Optional.of(resp);
        } finally {
            progress.release(req.id());
        }
    }

    private JsonRpcMessage dispatch(JsonRpcRequest req) {
        var handler = RequestMethod.from(req.method()).map(requests::get);
        if (handler.isEmpty()) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Unknown method: " + req.method());
        }
        try {
            var resp = handler.get().apply(req);
            if (resp == null) {
                throw new IllegalStateException("handler returned null");
            }
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
            progress.send(new ProgressNotification(token, current, 1.0, msg),
                    (m, payload) -> send(new JsonRpcNotification(m.method(), payload)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }
}

