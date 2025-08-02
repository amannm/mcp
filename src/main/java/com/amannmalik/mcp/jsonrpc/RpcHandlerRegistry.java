package com.amannmalik.mcp.jsonrpc;

import com.amannmalik.mcp.util.JsonRpcRequestProcessor;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;

import java.io.IOException;
import java.util.*;

public final class RpcHandlerRegistry {
    private final JsonRpcRequestProcessor processor;
    private final Map<RequestMethod, RequestHandler> requestHandlers = new EnumMap<>(RequestMethod.class);
    private final Map<NotificationMethod, NotificationHandler> notificationHandlers = new EnumMap<>(NotificationMethod.class);

    public RpcHandlerRegistry(JsonRpcRequestProcessor processor) {
        this.processor = processor;
    }

    public void register(RequestMethod method, RequestHandler handler) {
        requestHandlers.put(method, handler);
    }

    public void register(NotificationMethod method, NotificationHandler handler) {
        notificationHandlers.put(method, handler);
    }

    public Optional<JsonRpcMessage> handle(JsonRpcRequest req, boolean cancellable) throws IOException {
        return processor.process(req, cancellable, r -> {
            var method = RequestMethod.from(r.method());
            if (method.isEmpty()) {
                return JsonRpcError.of(r.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Unknown method: " + r.method());
            }
            var handler = requestHandlers.get(method.get());
            if (handler == null) {
                return JsonRpcError.of(r.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Unknown method: " + r.method());
            }
            return handler.handle(r);
        });
    }

    public void handle(JsonRpcNotification note) {
        var method = NotificationMethod.from(note.method());
        if (method.isEmpty()) return;
        var handler = notificationHandlers.get(method.get());
        if (handler != null) handler.handle(note);
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
