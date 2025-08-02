package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.jsonrpc.IdTracker;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

/**
 * Handles common request lifecycle concerns like progress tracking,
 * cancellation and id management.
 */
public final class JsonRpcRequestProcessor {
    private final ProgressManager progressManager;
    private final CancellationTracker cancellationTracker;
    private final NotificationSender sender;
    private final Optional<IdTracker> idTracker;

    public JsonRpcRequestProcessor(
            ProgressManager progressManager,
            CancellationTracker cancellationTracker,
            NotificationSender sender,
            Optional<IdTracker> idTracker
    ) {
        if (progressManager == null || cancellationTracker == null || sender == null || idTracker == null) {
            throw new IllegalArgumentException("manager, tracker, sender and idTracker required");
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
        this(progressManager, cancellationTracker, sender, Optional.empty());
    }

    /**
     * Process a request using the provided handler. The handler is expected to
     * perform the actual request logic and return a response or null. If the
     * request was cancelled the return value will be null.
     */
    public JsonRpcMessage process(
            JsonRpcRequest req,
            boolean cancellable,
            Function<JsonRpcRequest, JsonRpcMessage> handler
    ) throws IOException {
        if (req == null || handler == null) throw new IllegalArgumentException("request and handler required");
        try {
            idTracker.ifPresent(t -> t.register(req.id()));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_REQUEST, e.getMessage());
        }

        final Optional<ProgressToken> token;
        try {
            token = progressManager.register(req.id(), req.params());
            token.ifPresent(t -> sendProgress(t, 0.0));
        } catch (IllegalArgumentException e) {
            cleanup(req.id());
            return JsonRpcError.invalidParams(req.id(), e.getMessage());
        }

        if (cancellable) cancellationTracker.register(req.id());

        JsonRpcMessage resp;
        try {
            resp = handler.apply(req);
        } catch (IllegalArgumentException e) {
            resp = JsonRpcError.invalidParams(req.id(), e.getMessage());
        } catch (Exception e) {
            resp = JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }

        boolean cancelled = cancellable && cancellationTracker.isCancelled(req.id());
        if (!cancelled) token.ifPresent(t -> sendProgress(t, 1.0));
        cleanup(req.id());
        return cancelled ? null : resp;
    }

    private void cleanup(RequestId id) {
        progressManager.release(id);
        cancellationTracker.release(id);
        idTracker.ifPresent(t -> t.release(id));
    }

    private void sendProgress(ProgressToken token, double current) {
        try {
            progressManager.send(new ProgressNotification(token, current, 1.0, null), sender);
        } catch (IOException ignore) {
        }
    }
}
