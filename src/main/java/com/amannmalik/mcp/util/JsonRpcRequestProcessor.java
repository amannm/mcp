package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class JsonRpcRequestProcessor {
    private record RequestHandler(Function<JsonRpcRequest, JsonRpcMessage> fn, boolean cancellable) {}

    private final ProgressTracker progress;
    private final Consumer<JsonRpcNotification> sender;
    private final Map<String, RequestHandler> requests = new HashMap<>();
    private final Map<String, Consumer<JsonRpcNotification>> notifications = new HashMap<>();

    public JsonRpcRequestProcessor(ProgressTracker progress, Consumer<JsonRpcNotification> sender) {
        if (progress == null || sender == null) throw new IllegalArgumentException("progress and sender required");
        this.progress = progress;
        this.sender = sender;
    }

    public void registerRequest(String method, Function<JsonRpcRequest, JsonRpcMessage> handler, boolean cancellable) {
        requests.put(method, new RequestHandler(handler, cancellable));
    }

    public void registerNotification(String method, Consumer<JsonRpcNotification> handler) {
        notifications.put(method, handler);
    }

    public Optional<JsonRpcMessage> handle(JsonRpcRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        RequestHandler rh = requests.get(req.method());
        if (rh == null) {
            return Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Unknown method: " + req.method()));
        }
        Optional<ProgressToken> token;
        try {
            token = progress.register(req.id(), req.params(), rh.cancellable());
        } catch (IllegalArgumentException e) {
            progress.release(req.id());
            return Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_REQUEST, e.getMessage()));
        }
        try {
            token.ifPresent(t -> sendProgress(t, 0.0));
            if (progress.isCancelled(req.id())) return Optional.empty();
            JsonRpcMessage resp;
            try {
                resp = rh.fn().apply(req);
                if (resp == null) throw new IllegalStateException("handler returned null");
            } catch (IllegalArgumentException e) {
                return Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage()));
            } catch (Exception e) {
                return Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage()));
            }
            if (progress.isCancelled(req.id())) return Optional.empty();
            token.ifPresent(t -> sendProgress(t, 1.0));
            return Optional.of(resp);
        } finally {
            progress.release(req.id());
        }
    }

    public void handle(JsonRpcNotification note) {
        var handler = notifications.get(note.method());
        if (handler != null) handler.accept(note);
    }

    private void sendProgress(ProgressToken token, double current) {
        String msg = current >= 1.0 ? "completed" : "in progress";
        progress.send(new ProgressNotification(token, current, 1.0, msg), sender);
    }
}
