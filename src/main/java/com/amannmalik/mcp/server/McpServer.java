package com.amannmalik.mcp.server;

import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.InitializeRequest;
import com.amannmalik.mcp.lifecycle.InitializeResponse;
import com.amannmalik.mcp.lifecycle.LifecycleCodec;
import com.amannmalik.mcp.lifecycle.LifecycleState;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;

import java.io.EOFException;
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

    protected McpServer(Set<ServerCapability> capabilities, Transport transport) {
        this.transport = transport;
        this.lifecycle = new ProtocolLifecycle(EnumSet.copyOf(capabilities));

        registerRequestHandler("initialize", this::initialize);
        registerNotificationHandler("notifications/initialized", this::initialized);
        registerRequestHandler("ping", this::ping);
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
            JsonObject obj;
            try {
                obj = transport.receive();
            } catch (EOFException e) {
                lifecycle.shutdown();
                break;
            }
            JsonRpcMessage msg = JsonRpcCodec.fromJsonObject(obj);
            switch (msg) {
                case JsonRpcRequest req -> onRequest(req);
                case JsonRpcNotification note -> onNotification(note);
                default -> {
                }
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
        JsonRpcMessage resp = handler.handle(req);
        if (resp != null) send(resp);
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

    @Override
    public void close() throws IOException {
        lifecycle.shutdown();
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
