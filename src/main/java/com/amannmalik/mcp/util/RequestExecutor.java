package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;

import java.io.IOException;
import java.util.Optional;

/** Utility to execute request handlers with progress and cancellation support. */
public final class RequestExecutor {
    private RequestExecutor() {}

    @FunctionalInterface
    public interface Handler {
        JsonRpcMessage handle(JsonRpcRequest request) throws Exception;
    }

    public static JsonRpcMessage execute(
            JsonRpcRequest request,
            boolean cancellable,
            Handler handler,
            ProgressManager progressManager,
            CancellationTracker cancellationTracker,
            NotificationSender progressSender) {
        cancellationTracker.register(request.id());
        Optional<ProgressToken> token;
        try {
            token = progressManager.register(request.id(), request.params());
            token.ifPresent(t -> send(new ProgressNotification(t, 0.0, 1.0, null), progressManager, progressSender));
        } catch (IllegalArgumentException e) {
            cancellationTracker.release(request.id());
            progressManager.release(request.id());
            return JsonRpcError.invalidParams(request.id(), e.getMessage());
        }

        if (!cancellable) {
            cancellationTracker.release(request.id());
        }

        try {
            JsonRpcMessage resp = handler.handle(request);
            if (cancellable && cancellationTracker.isCancelled(request.id())) {
                return null;
            }
            token.ifPresent(t -> send(new ProgressNotification(t, 1.0, 1.0, null), progressManager, progressSender));
            return resp;
        } catch (IllegalArgumentException e) {
            return JsonRpcError.invalidParams(request.id(), e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(request.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        } finally {
            progressManager.release(request.id());
            if (cancellable) {
                cancellationTracker.release(request.id());
            }
        }
    }

    private static void send(ProgressNotification note, ProgressManager manager, NotificationSender sender) {
        try {
            manager.send(note, sender);
        } catch (IOException ignore) {
        }
    }
}
