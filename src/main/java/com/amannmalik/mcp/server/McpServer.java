package com.amannmalik.mcp.server;

import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.InitializeRequest;
import com.amannmalik.mcp.lifecycle.InitializeResponse;
import com.amannmalik.mcp.lifecycle.LifecycleCodec;
import com.amannmalik.mcp.lifecycle.LifecycleState;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.*;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Basic framework for implementing MCP servers. */
public abstract class McpServer implements AutoCloseable {
    private final Transport transport;
    private final ProtocolLifecycle lifecycle;
    private final Map<String, RequestHandler> requestHandlers = new ConcurrentHashMap<>();
    private final Map<String, NotificationHandler> notificationHandlers = new ConcurrentHashMap<>();
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final Map<RequestId, ProgressToken> progressTokens = new ConcurrentHashMap<>();
    private final CancellationTracker cancellationTracker = new CancellationTracker();

    protected McpServer(Set<ServerCapability> capabilities, Transport transport) {
        this.transport = transport;
        this.lifecycle = new ProtocolLifecycle(EnumSet.copyOf(capabilities));

        registerRequestHandler("initialize", this::initialize);
        registerNotificationHandler("notifications/initialized", this::initialized);
        registerRequestHandler("ping", this::ping);
        registerNotificationHandler("notifications/cancelled", this::cancelled);
    }

    protected final ProtocolLifecycle lifecycle() {
        return lifecycle;
    }

    protected final void registerRequestHandler(String method, RequestHandler handler) {
        requestHandlers.put(method, handler);
    }

    protected final void registerNotificationHandler(String method, NotificationHandler handler) {
        notificationHandlers.put(method, handler);
    }

    public final void serve() throws IOException {
        while (lifecycle.state() != LifecycleState.SHUTDOWN) {
            JsonObject obj = transport.receive();
            JsonRpcMessage msg = JsonRpcCodec.fromJsonObject(obj);
            switch (msg) {
                case JsonRpcRequest req -> onRequest(req);
                case JsonRpcNotification note -> onNotification(note);
                default -> {}
            }
        }
    }

    protected void onRequest(JsonRpcRequest req) throws IOException {
        var handler = requestHandlers.get(req.method());
        if (handler == null) {
            send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                    "Unknown method: " + req.method(), null)));
            return;
        }
        ProgressToken token = parseProgressToken(req.params());
        if (token != null) {
            progressTracker.register(token);
            progressTokens.put(req.id(), token);
        }
        cancellationTracker.register(req.id());
        JsonRpcMessage resp = handler.handle(req);
        if (!cancellationTracker.isCancelled(req.id()) && resp != null) {
            send(resp);
        }
        cleanup(req.id());
    }

    protected void onNotification(JsonRpcNotification note) throws IOException {
        var handler = notificationHandlers.get(note.method());
        if (handler != null) handler.handle(note);
    }

    private JsonRpcMessage initialize(JsonRpcRequest req) {
        InitializeRequest init = LifecycleCodec.toInitializeRequest(req.params());
        InitializeResponse resp = lifecycle.initialize(init);
        return new JsonRpcResponse(req.id(), LifecycleCodec.toJsonObject(resp));
    }

    private void initialized(JsonRpcNotification note) {
        lifecycle.initialized();
    }

    private JsonRpcMessage ping(JsonRpcRequest req) {
        return new JsonRpcResponse(req.id(), jakarta.json.Json.createObjectBuilder().build());
    }

    protected final void send(JsonRpcMessage msg) throws IOException {
        transport.send(JsonRpcCodec.toJsonObject(msg));
    }

    protected final void requireClientCapability(ClientCapability cap) {
        if (!lifecycle.negotiatedClientCapabilities().contains(cap)) {
            throw new IllegalStateException("Missing client capability: " + cap);
        }
    }

    protected final void requireServerCapability(ServerCapability cap) {
        if (!lifecycle.serverCapabilities().contains(cap)) {
            throw new IllegalStateException("Server capability not declared: " + cap);
        }
    }

    private ProgressToken parseProgressToken(JsonObject params) {
        if (params == null || !params.containsKey("_meta")) return null;
        JsonObject meta = params.getJsonObject("_meta");
        if (!meta.containsKey("progressToken")) return null;
        var val = meta.get("progressToken");
        return switch (val.getValueType()) {
            case STRING -> new ProgressToken.StringToken(meta.getString("progressToken"));
            case NUMBER -> new ProgressToken.NumericToken(meta.getJsonNumber("progressToken").longValue());
            default -> null;
        };
    }

    private void cancelled(JsonRpcNotification note) {
        CancelledNotification cn = CancellationCodec.toCancelledNotification(note.params());
        cancellationTracker.cancel(cn.requestId(), cn.reason());
        ProgressToken token = progressTokens.get(cn.requestId());
        if (token != null) {
            progressTracker.release(token);
        }
    }

    private void cleanup(RequestId id) {
        ProgressToken token = progressTokens.remove(id);
        if (token != null) progressTracker.release(token);
        cancellationTracker.release(id);
    }

    protected final void sendProgress(ProgressNotification note) throws IOException {
        send(new JsonRpcNotification("notifications/progress", ProgressCodec.toJsonObject(note)));
    }

    @Override
    public void close() throws IOException {
        transport.close();
    }

    @FunctionalInterface
    protected interface RequestHandler {
        JsonRpcMessage handle(JsonRpcRequest request);
    }

    @FunctionalInterface
    protected interface NotificationHandler {
        void handle(JsonRpcNotification notification) throws IOException;
    }
}
