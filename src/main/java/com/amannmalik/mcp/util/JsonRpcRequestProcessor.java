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
        if (idTracker != null) {
            try {
                idTracker.register(req.id());
            } catch (IllegalArgumentException e) {
                return Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_REQUEST, e.getMessage()));
            }
        }

        final Optional<ProgressToken> token;
        try {
            token = progressManager.register(req.id(), req.params());
            token.ifPresent(t -> sendProgress(t, 0.0));
        } catch (IllegalArgumentException e) {
            cleanup(req.id());
            return Optional.of(JsonRpcError.invalidParams(req.id(), e.getMessage()));
        }

        try {
            if (cancellable) {
                cancellationTracker.register(req.id());
                if (cancellationTracker.isCancelled(req.id())) {
                    return Optional.empty();
                }
            }

            JsonRpcMessage resp;
            try {
                resp = handler.apply(req);
                if (resp == null) throw new IllegalStateException("handler returned null");
            } catch (IllegalArgumentException e) {
                resp = JsonRpcError.invalidParams(req.id(), e.getMessage());
            } catch (Exception e) {
                resp = JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
            }

            if (cancellable && cancellationTracker.isCancelled(req.id())) return Optional.empty();
            token.ifPresent(t -> sendProgress(t, 1.0));
            return Optional.of(resp);
        } finally {
            cleanup(req.id());
        }
    }

    private void cleanup(RequestId id) {
        progressManager.release(id);
        cancellationTracker.release(id);
        if (idTracker != null) idTracker.release(id);
    }

    private void sendProgress(ProgressToken token, double current) {
        try {
            progressManager.send(new ProgressNotification(token, current, 1.0, null), sender);
        } catch (IOException ignore) {
        }
    }
}
