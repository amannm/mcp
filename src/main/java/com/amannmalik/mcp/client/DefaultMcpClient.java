package com.amannmalik.mcp.client;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.client.roots.*;
import jakarta.json.JsonObject;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class DefaultMcpClient implements McpClient {
    private final ClientInfo info;
    private final Set<ClientCapability> capabilities;
    private final Transport transport;
    private final RootsProvider roots;
    private RootsSubscription rootsSub;
    private final AtomicLong id = new AtomicLong(1);
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private Thread reader;
    private volatile boolean connected;
    private Set<ServerCapability> serverCapabilities = Set.of();
    private String instructions;

    public DefaultMcpClient(ClientInfo info,
                            Set<ClientCapability> capabilities,
                            Transport transport) {
        this(info, capabilities, transport, null);
    }

    public DefaultMcpClient(ClientInfo info,
                            Set<ClientCapability> capabilities,
                            Transport transport,
                            RootsProvider roots) {
        this.info = info;
        this.capabilities = capabilities.isEmpty() ? Set.of() : EnumSet.copyOf(capabilities);
        this.transport = transport;
        this.roots = roots;
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
        if (roots != null) {
            rootsSub = roots.subscribe(() -> {
                if (connected) {
                    try {
                        notify("notifications/roots/list_changed",
                                RootsCodec.toJsonObject(new RootsListChangedNotification()));
                    } catch (IOException ignore) {}
                }
            });
        }
        reader = new Thread(this::readLoop);
        reader.setDaemon(true);
        reader.start();
    }

    @Override
    public synchronized void disconnect() throws IOException {
        if (!connected) return;
        connected = false;
        if (rootsSub != null) {
            rootsSub.close();
            rootsSub = null;
        }
        if (roots != null) {
            roots.close();
        }
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

    @Override
    public JsonRpcMessage request(String method, JsonObject params) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        RequestId reqId = new RequestId.NumericId(id.getAndIncrement());
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(reqId, future);
        try {
            transport.send(JsonRpcCodec.toJsonObject(new JsonRpcRequest(reqId, method, params)));
        } catch (IOException e) {
            pending.remove(reqId);
            throw e;
        }
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

    @Override
    public void notify(String method, JsonObject params) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        JsonRpcNotification notification = new JsonRpcNotification(method, params);
        transport.send(JsonRpcCodec.toJsonObject(notification));
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
                case JsonRpcRequest req -> handleRequest(req);
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

    private void handleRequest(JsonRpcRequest req) {
        try {
            if ("roots/list".equals(req.method())) {
                if (roots == null) {
                    JsonRpcError err = new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                            JsonRpcErrorCode.METHOD_NOT_FOUND.code(), "Roots not supported", null));
                    transport.send(JsonRpcCodec.toJsonObject(err));
                    return;
                }
                ListRootsResponse resp = new ListRootsResponse(roots.list());
                JsonRpcResponse r = new JsonRpcResponse(req.id(), RootsCodec.toJsonObject(resp));
                transport.send(JsonRpcCodec.toJsonObject(r));
                return;
            }
            JsonRpcError err = new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.METHOD_NOT_FOUND.code(), "Unknown method: " + req.method(), null));
            transport.send(JsonRpcCodec.toJsonObject(err));
        } catch (Exception e) {
            try {
                JsonRpcError err = new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
                transport.send(JsonRpcCodec.toJsonObject(err));
            } catch (IOException ignore) {}
        }
    }
}
