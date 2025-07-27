package com.amannmalik.mcp.client;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/** Minimal implementation of an MCP client. */
public final class SimpleMcpClient implements McpClient {
    private final ClientInfo info;
    private final Set<ClientCapability> capabilities;
    private final Transport transport;
    private final AtomicLong id = new AtomicLong(1);
    private volatile boolean connected;
    private Set<ServerCapability> serverCapabilities = Set.of();
    private String instructions;

    public SimpleMcpClient(ClientInfo info, Set<ClientCapability> capabilities, Transport transport) {
        this.info = info;
        this.capabilities = capabilities.isEmpty() ? Set.of() : EnumSet.copyOf(capabilities);
        this.transport = transport;
    }

    @Override
    public ClientInfo info() {
        return info;
    }

    @Override
    public synchronized void connect() throws IOException {
        if (connected) return;
        InitializeRequest init = new InitializeRequest(
                ProtocolLifecycle.SUPPORTED_VERSION,
                new Capabilities(capabilities, Set.of()),
                info
        );
        RequestId reqId = new RequestId.NumericId(id.getAndIncrement());
        JsonRpcRequest request = new JsonRpcRequest(reqId, "initialize", LifecycleCodec.toJsonObject(init));
        transport.send(JsonRpcCodec.toJsonObject(request));
        JsonRpcMessage msg = JsonRpcCodec.fromJsonObject(transport.receive());
        if (msg instanceof JsonRpcResponse resp) {
            InitializeResponse ir = LifecycleCodec.toInitializeResponse(resp.result());
            serverCapabilities = ir.capabilities().server();
            instructions = ir.instructions();
        } else if (msg instanceof JsonRpcError err) {
            throw new IOException("Initialization failed: " + err.error().message());
        } else {
            throw new IOException("Unexpected message type: " + msg.getClass().getSimpleName());
        }
        JsonRpcNotification note = new JsonRpcNotification("notifications/initialized", null);
        transport.send(JsonRpcCodec.toJsonObject(note));
        connected = true;
    }

    @Override
    public synchronized void disconnect() throws IOException {
        if (!connected) return;
        connected = false;
        transport.close();
    }

    @Override
    public boolean connected() {
        return connected;
    }

    @Override
    public String context() {
        return instructions == null ? "" : instructions;
    }

    public JsonRpcMessage request(String method, JsonObject params) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        RequestId reqId = new RequestId.NumericId(id.getAndIncrement());
        JsonRpcRequest req = new JsonRpcRequest(reqId, method, params);
        transport.send(JsonRpcCodec.toJsonObject(req));
        while (true) {
            JsonRpcMessage msg = JsonRpcCodec.fromJsonObject(transport.receive());
            if (msg instanceof JsonRpcResponse resp && resp.id().equals(reqId)) {
                return resp;
            }
            if (msg instanceof JsonRpcError err && err.id().equals(reqId)) {
                return err;
            }
        }
    }

    public Set<ServerCapability> serverCapabilities() {
        return serverCapabilities;
    }
}
