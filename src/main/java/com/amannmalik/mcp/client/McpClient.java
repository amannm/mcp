package com.amannmalik.mcp.client;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicLong;

/** Basic client for interacting with a single MCP server. */
public final class McpClient implements AutoCloseable {
    private final Transport transport;
    private final AtomicLong ids = new AtomicLong(1);
    private final IdTracker tracker = new IdTracker();

    private LifecycleState state = LifecycleState.INIT;
    private Capabilities capabilities;
    private ServerInfo serverInfo;
    private String protocolVersion;

    public McpClient(Transport transport) {
        this.transport = transport;
    }

    public synchronized InitializeResponse initialize(Capabilities clientCaps, ClientInfo info) throws IOException {
        ensureState(LifecycleState.INIT);
        RequestId id = nextId();
        InitializeRequest req = new InitializeRequest(ProtocolLifecycle.SUPPORTED_VERSION, clientCaps, info);
        JsonRpcRequest rpc = new JsonRpcRequest(id, "initialize", LifecycleCodec.toJson(req));
        transport.send(JsonRpcCodec.toJsonObject(rpc));
        JsonRpcMessage msg = JsonRpcCodec.fromJsonObject(transport.receive());
        if (!(msg instanceof JsonRpcResponse resp) || !resp.id().equals(id)) {
            throw new IOException("Invalid initialize response");
        }
        InitializeResponse init = LifecycleCodec.toInitializeResponse(resp.result());
        protocolVersion = init.protocolVersion();
        capabilities = init.capabilities();
        serverInfo = init.serverInfo();
        transport.send(JsonRpcCodec.toJsonObject(new JsonRpcNotification("notifications/initialized", null)));
        state = LifecycleState.OPERATION;
        return init;
    }

    public synchronized JsonRpcMessage call(String method, JsonObject params) throws IOException {
        ensureState(LifecycleState.OPERATION);
        RequestId id = nextId();
        JsonRpcRequest req = new JsonRpcRequest(id, method, params);
        transport.send(JsonRpcCodec.toJsonObject(req));
        while (true) {
            JsonRpcMessage msg = JsonRpcCodec.fromJsonObject(transport.receive());
            if (msg instanceof JsonRpcResponse resp && resp.id().equals(id)) return resp;
            if (msg instanceof JsonRpcError err && err.id().equals(id)) return err;
        }
    }

    public LifecycleState state() {
        return state;
    }

    @Override
    public synchronized void close() throws IOException {
        state = LifecycleState.SHUTDOWN;
        transport.close();
    }

    private RequestId nextId() {
        RequestId id = new RequestId.NumericId(ids.getAndIncrement());
        tracker.register(id);
        return id;
    }

    private void ensureState(LifecycleState expected) {
        if (state != expected) throw new IllegalStateException("Invalid state: " + state);
    }
}
