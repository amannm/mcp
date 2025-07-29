package com.amannmalik.mcp.client;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

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
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private Thread reader;
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
            if (!ProtocolLifecycle.SUPPORTED_VERSION.equals(ir.protocolVersion())) {
                throw new UnsupportedProtocolVersionException(ir.protocolVersion(), ProtocolLifecycle.SUPPORTED_VERSION);
            }
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
        reader = new Thread(this::readLoop);
        reader.setDaemon(true);
        reader.start();
    }

    @Override
    public synchronized void disconnect() throws IOException {
        if (!connected) return;
        connected = false;
        transport.close();
        if (reader != null) {
            try { reader.join(100); } catch (InterruptedException ignore) { Thread.currentThread().interrupt(); }
            reader = null;
        }
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
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(reqId, future);
        transport.send(JsonRpcCodec.toJsonObject(new JsonRpcRequest(reqId, method, params)));
        try {
            return future.get(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IOException("Request timed out after 30 seconds");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        } finally {
            pending.remove(reqId);
        }
    }

    public Set<ServerCapability> serverCapabilities() {
        return serverCapabilities;
    }

    private void readLoop() {
        while (connected) {
            JsonRpcMessage msg;
            try {
                msg = JsonRpcCodec.fromJsonObject(transport.receive());
            } catch (IOException e) {
                pending.values().forEach(f -> f.completeExceptionally(e));
                break;
            }
            switch (msg) {
                case JsonRpcResponse resp -> {
                    CompletableFuture<JsonRpcMessage> f = pending.remove(resp.id());
                    if (f != null) f.complete(resp);
                }
                case JsonRpcError err -> {
                    CompletableFuture<JsonRpcMessage> f = pending.remove(err.id());
                    if (f != null) f.complete(err);
                }
                default -> {
                    // ignore notifications
                }
            }
        }
    }
}
