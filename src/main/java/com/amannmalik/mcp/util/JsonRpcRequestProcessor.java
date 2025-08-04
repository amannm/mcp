package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class JsonRpcRequestProcessor {
    private final RequestTracker tracker;
    private final NotificationSender sender;
    private final Map<String, Function<JsonRpcRequest, JsonRpcMessage>> requests = new HashMap<>();
    private final Map<String, Consumer<JsonRpcNotification>> notifications = new HashMap<>();

    public JsonRpcRequestProcessor(RequestTracker tracker, NotificationSender sender) {
        if (tracker == null || sender == null) throw new IllegalArgumentException("tracker and sender required");
        this.tracker = tracker;
        this.sender = sender;
    }

    public void registerRequest(String method, Function<JsonRpcRequest, JsonRpcMessage> handler) {
        requests.put(method, handler);
    }

    public void registerNotification(String method, Consumer<JsonRpcNotification> handler) {
        notifications.put(method, handler);
    }

    public Optional<JsonRpcMessage> handle(JsonRpcRequest req, boolean cancellable) {
        if (req == null) throw new IllegalArgumentException("request required");

        final Optional<ProgressToken> token;
        try {
            token = tracker.register(req.id(), req.params());
        } catch (IllegalArgumentException e) {
            cleanup(req.id());
            return Optional.of(JsonRpcError.invalidParams(req.id(), e.getMessage()));
        }

        try {
            token.ifPresent(t -> sendProgress(t, 0.0));
            if (cancellable && tracker.isCancelled(req.id())) return Optional.empty();
            JsonRpcMessage resp = dispatch(req);
            if (cancellable && tracker.isCancelled(req.id())) return Optional.empty();
            token.ifPresent(t -> sendProgress(t, 1.0));
            return Optional.of(resp);
        } finally {
            cleanup(req.id());
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
        tracker.release(id);
    }

    private void sendProgress(ProgressToken token, double current) {
        String msg = current >= 1.0 ? "completed" : "in progress";
        try {
            tracker.send(new ProgressNotification(token, current, 1.0, msg), sender);
        } catch (IOException ignore) {
        }
    }
}
