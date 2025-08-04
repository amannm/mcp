package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class JsonRpcRequestProcessor {
    private final ProgressManager progress;
    private final NotificationSender sender;
    private final IdTracker idTracker;
    private final Map<String, Function<JsonRpcRequest, JsonRpcMessage>> requests = new HashMap<>();
    private final Map<String, Consumer<JsonRpcNotification>> notifications = new HashMap<>();

    public JsonRpcRequestProcessor(ProgressManager progress, NotificationSender sender, IdTracker idTracker) {
        if (progress == null || sender == null) throw new IllegalArgumentException("progress and sender required");
        this.progress = progress;
        this.sender = sender;
        this.idTracker = idTracker;
    }

    public JsonRpcRequestProcessor(ProgressManager progress, NotificationSender sender) {
        this(progress, sender, null);
    }

    public void registerRequest(String method, Function<JsonRpcRequest, JsonRpcMessage> handler) {
        requests.put(method, handler);
    }

    public void registerNotification(String method, Consumer<JsonRpcNotification> handler) {
        notifications.put(method, handler);
    }

    public Optional<JsonRpcMessage> handle(JsonRpcRequest req, boolean cancellable) {
        if (req == null) throw new IllegalArgumentException("request required");

        Optional<JsonRpcMessage> idError = registerId(req.id());
        if (idError.isPresent()) return idError;

        final Optional<ProgressToken> token;
        try {
            token = progress.register(req.id(), req.params());
        } catch (IllegalArgumentException e) {
            cleanup(req.id());
            return Optional.of(JsonRpcError.invalidParams(req.id(), e.getMessage()));
        }

        try {
            token.ifPresent(t -> sendProgress(t, 0.0));
            if (cancellable && progress.isCancelled(req.id())) return Optional.empty();
            JsonRpcMessage resp = dispatch(req);
            if (cancellable && progress.isCancelled(req.id())) return Optional.empty();
            token.ifPresent(t -> sendProgress(t, 1.0));
            return Optional.of(resp);
        } finally {
            cleanup(req.id());
        }
    }

    private Optional<JsonRpcMessage> registerId(RequestId id) {
        if (idTracker == null) return Optional.empty();
        try {
            idTracker.register(id);
            return Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.of(JsonRpcError.of(id, JsonRpcErrorCode.INVALID_REQUEST, e.getMessage()));
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
            return JsonRpcError.invalidParams(req.id(), e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    public void handle(JsonRpcNotification note) {
        var handler = notifications.get(note.method());
        if (handler != null) handler.accept(note);
    }

    private void cleanup(RequestId id) {
        progress.release(id);
        if (idTracker != null) idTracker.release(id);
    }

    private void sendProgress(ProgressToken token, double current) {
        String msg = current >= 1.0 ? "completed" : "in progress";
        try {
            progress.send(new ProgressNotification(token, current, 1.0, msg), sender);
        } catch (IOException ignore) {
        }
    }
}
