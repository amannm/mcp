package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.*;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public final class JsonRpcRequestProcessor {
    private final ProgressManager progressManager;
    private final CancellationTracker cancellationTracker;
    private final NotificationSender sender;
    private final IdTracker idTracker;

    public JsonRpcRequestProcessor(
            ProgressManager progressManager,
            CancellationTracker cancellationTracker,
            NotificationSender sender,
            IdTracker idTracker
    ) {
        if (progressManager == null || cancellationTracker == null || sender == null) {
            throw new IllegalArgumentException("manager, tracker and sender required");
        }
        this.progressManager = progressManager;
        this.cancellationTracker = cancellationTracker;
        this.sender = sender;
        this.idTracker = idTracker;
    }

    public JsonRpcRequestProcessor(
            ProgressManager progressManager,
            CancellationTracker cancellationTracker,
            NotificationSender sender
    ) {
        this(progressManager, cancellationTracker, sender, null);
    }

    public Optional<JsonRpcMessage> process(
            JsonRpcRequest req,
            boolean cancellable,
            Function<JsonRpcRequest, JsonRpcMessage> handler
    ) {
        if (req == null || handler == null) throw new IllegalArgumentException("request and handler required");

        Optional<JsonRpcMessage> idError = registerId(req.id());
        if (idError.isPresent()) return idError;

        final Optional<ProgressToken> token;
        try {
            token = progressManager.register(req.id(), req.params());
        } catch (IllegalArgumentException e) {
            cleanup(req.id());
            return Optional.of(JsonRpcError.invalidParams(req.id(), e.getMessage()));
        }

        try {
            token.ifPresent(t -> sendProgress(t, 0.0));
            if (cancellable && registerCancellation(req.id())) return Optional.empty();
            JsonRpcMessage resp = handle(req, handler);
            if (cancellable && cancellationTracker.isCancelled(req.id())) return Optional.empty();
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

    private boolean registerCancellation(RequestId id) {
        cancellationTracker.register(id);
        return cancellationTracker.isCancelled(id);
    }

    private JsonRpcMessage handle(JsonRpcRequest req, Function<JsonRpcRequest, JsonRpcMessage> handler) {
        try {
            var resp = handler.apply(req);
            if (resp == null) throw new IllegalStateException("handler returned null");
            return resp;
        } catch (IllegalArgumentException e) {
            return JsonRpcError.invalidParams(req.id(), e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private void cleanup(RequestId id) {
        progressManager.release(id);
        cancellationTracker.release(id);
        if (idTracker != null) idTracker.release(id);
    }

    private void sendProgress(ProgressToken token, double current) {
        try {
            String msg = current >= 1.0 ? "complete" : "in progress";
            progressManager.send(new ProgressNotification(token, current, 1.0, msg), sender);
        } catch (IOException ignore) {
        }
    }
}
