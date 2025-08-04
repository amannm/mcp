package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;

import java.io.IOException;
import java.util.*;

public final class JsonRpcRequestProcessor {
    private final ProgressTracker tracker;
    private final NotificationSender sender;
    private final IdTracker idTracker;
    private final Map<RequestMethod, RequestHandler> requests = new EnumMap<>(RequestMethod.class);
    private final Map<NotificationMethod, NotificationHandler> notifications = new EnumMap<>(NotificationMethod.class);

    public JsonRpcRequestProcessor(ProgressTracker tracker, NotificationSender sender, IdTracker idTracker) {
        if (tracker == null || sender == null) throw new IllegalArgumentException("tracker and sender required");
        this.tracker = tracker;
        this.sender = sender;
        this.idTracker = idTracker;
    }

    public JsonRpcRequestProcessor(ProgressTracker tracker, NotificationSender sender) {
        this(tracker, sender, null);
    }

    public void register(RequestMethod method, RequestHandler handler) {
        requests.put(method, handler);
    }

    public void register(NotificationMethod method, NotificationHandler handler) {
        notifications.put(method, handler);
    }

    public Optional<JsonRpcMessage> handle(JsonRpcRequest req, boolean cancellable) {
        if (req == null) throw new IllegalArgumentException("request required");

        Optional<JsonRpcMessage> idError = registerId(req.id());
        if (idError.isPresent()) return idError;

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
            var method = RequestMethod.from(req.method());
            if (method.isEmpty()) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Unknown method: " + req.method());
            }
            var handler = requests.get(method.get());
            if (handler == null) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Unknown method: " + req.method());
            }
            var resp = handler.handle(req);
            if (resp == null) throw new IllegalStateException("handler returned null");
            return resp;
        } catch (IllegalArgumentException e) {
            return JsonRpcError.invalidParams(req.id(), e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    public void handle(JsonRpcNotification note) {
        var method = NotificationMethod.from(note.method());
        if (method.isEmpty()) return;
        var handler = notifications.get(method.get());
        if (handler != null) handler.handle(note);
    }

    private void cleanup(RequestId id) {
        tracker.release(id);
        if (idTracker != null) idTracker.release(id);
    }

    private void sendProgress(ProgressToken token, double current) {
        String msg = current >= 1.0 ? "completed" : "in progress";
        try {
            tracker.send(new ProgressNotification(token, current, 1.0, msg), sender);
        } catch (IOException ignore) {
        }
    }
    @FunctionalInterface
    public interface RequestHandler {
        JsonRpcMessage handle(JsonRpcRequest request);
    }

    @FunctionalInterface
    public interface NotificationHandler {
        void handle(JsonRpcNotification notification);
    }
}
