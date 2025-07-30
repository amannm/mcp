package com.amannmalik.mcp.client;

import com.amannmalik.mcp.client.elicitation.ElicitationAction;
import com.amannmalik.mcp.client.elicitation.ElicitationCodec;
import com.amannmalik.mcp.client.elicitation.ElicitationProvider;
import com.amannmalik.mcp.client.elicitation.ElicitationRequest;
import com.amannmalik.mcp.client.elicitation.ElicitationResponse;
import com.amannmalik.mcp.client.roots.RootsCodec;
import com.amannmalik.mcp.client.roots.RootsProvider;
import com.amannmalik.mcp.client.roots.RootsSubscription;
import com.amannmalik.mcp.client.sampling.CreateMessageRequest;
import com.amannmalik.mcp.client.sampling.CreateMessageResponse;
import com.amannmalik.mcp.client.sampling.SamplingCodec;
import com.amannmalik.mcp.client.sampling.SamplingProvider;
import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.lifecycle.Capabilities;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.lifecycle.InitializeRequest;
import com.amannmalik.mcp.lifecycle.InitializeResponse;
import com.amannmalik.mcp.lifecycle.LifecycleCodec;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.lifecycle.ServerFeatures;
import com.amannmalik.mcp.lifecycle.UnsupportedProtocolVersionException;
import com.amannmalik.mcp.ping.PingCodec;
import com.amannmalik.mcp.ping.PingMonitor;
import com.amannmalik.mcp.ping.PingResponse;
import com.amannmalik.mcp.server.logging.LoggingCodec;
import com.amannmalik.mcp.server.logging.LoggingLevel;
import com.amannmalik.mcp.server.logging.LoggingListener;
import com.amannmalik.mcp.server.logging.SetLevelRequest;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.CancellationCodec;
import com.amannmalik.mcp.util.CancelledNotification;
import com.amannmalik.mcp.util.CancellationTracker;
import com.amannmalik.mcp.util.ProgressNotification;
import com.amannmalik.mcp.util.ProgressToken;
import com.amannmalik.mcp.util.ProgressTracker;
import com.amannmalik.mcp.security.RateLimiter;
import com.amannmalik.mcp.util.ProgressCodec;
import com.amannmalik.mcp.util.ProgressListener;
import com.amannmalik.mcp.validation.MetaValidator;
import com.amannmalik.mcp.validation.SchemaValidator;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public final class McpClient implements AutoCloseable {
    private final ClientInfo info;
    private final Set<ClientCapability> capabilities;
    private final Transport transport;
    private final SamplingProvider sampling;
    private final RootsProvider roots;
    private RootsSubscription rootsSubscription;
    private final boolean rootsListChangedSupported;
    private final ElicitationProvider elicitation;
    private final AtomicLong id = new AtomicLong(1);
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private final CancellationTracker cancellationTracker = new CancellationTracker();
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final Map<RequestId, ProgressToken> progressTokens = new ConcurrentHashMap<>();
    private final RateLimiter progressLimiter = new RateLimiter(20, 1000);
    private Thread reader;
    private ScheduledExecutorService pinger;
    private long pingInterval;
    private long pingTimeout;
    private volatile boolean connected;
    private Set<ServerCapability> serverCapabilities = Set.of();
    private String instructions;
    private boolean resourcesSubscribeSupported;
    private boolean resourcesListChangedSupported;
    private boolean toolsListChangedSupported;
    private boolean promptsListChangedSupported;
    private ProgressListener progressListener = n -> {
    };
    private LoggingListener loggingListener = n -> {
    };

    public void configurePing(long intervalMillis, long timeoutMillis) {
        if (connected) throw new IllegalStateException("already connected");
        if (intervalMillis < 0 || timeoutMillis <= 0) throw new IllegalArgumentException("invalid ping settings");
        this.pingInterval = intervalMillis;
        this.pingTimeout = timeoutMillis;
    }

    public McpClient(ClientInfo info, Set<ClientCapability> capabilities, Transport transport) {
        this(info, capabilities, transport, null, null, null);
    }

    public McpClient(ClientInfo info,
                     Set<ClientCapability> capabilities,
                     Transport transport,
                    SamplingProvider sampling,
                    RootsProvider roots,
                    ElicitationProvider elicitation) {
        this.info = info;
        this.capabilities = capabilities.isEmpty() ? Set.of() : EnumSet.copyOf(capabilities);
        this.transport = transport;
        this.sampling = sampling;
        this.roots = roots;
        this.rootsListChangedSupported = roots != null && roots.supportsListChanged();
        this.elicitation = elicitation;
        if (this.capabilities.contains(ClientCapability.SAMPLING) && this.sampling == null) {
            throw new IllegalArgumentException("sampling capability requires provider");
        }
        if (this.capabilities.contains(ClientCapability.ELICITATION) && this.elicitation == null) {
            throw new IllegalArgumentException("elicitation capability requires provider");
        }
        this.pingInterval = 0;
        this.pingTimeout = 5000;
    }

    public ClientInfo info() {
        return info;
    }

    public synchronized void connect() throws IOException {
        if (connected) return;
        InitializeRequest init = new InitializeRequest(
                ProtocolLifecycle.SUPPORTED_VERSION,
                new Capabilities(capabilities, Set.of(), Map.of(), Map.of()),
                info
        );
        var initJson = LifecycleCodec.toJsonObject(init);
        if (capabilities.contains(ClientCapability.ROOTS) && !rootsListChangedSupported) {
            var caps = initJson.getJsonObject("capabilities");
            if (caps != null && caps.containsKey("roots")) {
                var rootsCaps = caps.getJsonObject("roots");
                rootsCaps = jakarta.json.Json.createObjectBuilder(rootsCaps)
                        .add("listChanged", false)
                        .build();
                caps = jakarta.json.Json.createObjectBuilder(caps)
                        .add("roots", rootsCaps)
                        .build();
                initJson = jakarta.json.Json.createObjectBuilder(initJson)
                        .add("capabilities", caps)
                        .build();
            }
        }
        RequestId reqId = new RequestId.NumericId(id.getAndIncrement());
        JsonRpcRequest request = new JsonRpcRequest(reqId, "initialize", initJson);
        transport.send(JsonRpcCodec.toJsonObject(request));
        CompletableFuture<JsonRpcMessage> future = CompletableFuture.supplyAsync(() -> {
            try {
                return JsonRpcCodec.fromJsonObject(transport.receive());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        JsonRpcMessage msg;
        try {
            msg = future.get(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            try {
                transport.close();
            } catch (IOException ignore) {
            }
            throw new IOException("Initialization timed out after " + DEFAULT_TIMEOUT + " ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        }
        if (msg instanceof JsonRpcResponse resp) {
            InitializeResponse ir = LifecycleCodec.toInitializeResponse(resp.result());
            if (!ProtocolLifecycle.SUPPORTED_VERSION.equals(ir.protocolVersion())) {
                try {
                    transport.close();
                } catch (IOException ignore) {
                }
                throw new UnsupportedProtocolVersionException(ir.protocolVersion(), ProtocolLifecycle.SUPPORTED_VERSION);
            }
            serverCapabilities = ir.capabilities().server();
            instructions = ir.instructions();
            ServerFeatures f = ir.features();
            if (f != null) {
                resourcesSubscribeSupported = f.resourcesSubscribe();
                resourcesListChangedSupported = f.resourcesListChanged();
                toolsListChangedSupported = f.toolsListChanged();
                promptsListChangedSupported = f.promptsListChanged();
            }
        } else if (msg instanceof JsonRpcError err) {
            throw new IOException("Initialization failed: " + err.error().message());
        } else {
            throw new IOException("Unexpected message type: " + msg.getClass().getSimpleName());
        }
        JsonRpcNotification note = new JsonRpcNotification("notifications/initialized", null);
        transport.send(JsonRpcCodec.toJsonObject(note));
        connected = true;
        if (roots != null && capabilities.contains(ClientCapability.ROOTS) && rootsListChangedSupported) {
            try {
                rootsSubscription = roots.subscribe(() -> {
                    try {
                        notify("notifications/roots/list_changed", null);
                    } catch (IOException ignore) {
                    }
                });
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        reader = new Thread(this::readLoop);
        reader.setDaemon(true);
        reader.start();
        if (pingInterval > 0) {
            pinger = Executors.newSingleThreadScheduledExecutor();
            pinger.scheduleAtFixedRate(() -> {
                if (!PingMonitor.isAlive(this, pingTimeout)) {
                    try {
                        disconnect();
                    } catch (IOException ignore) {
                    }
                    if (System.err != null) System.err.println("Ping failed, connection closed");
                }
            }, pingInterval, pingInterval, TimeUnit.MILLISECONDS);
        }
    }

    public synchronized void disconnect() throws IOException {
        if (!connected) return;
        connected = false;
        if (pinger != null) {
            pinger.shutdownNow();
            pinger = null;
        }
        transport.close();
        if (rootsSubscription != null) {
            rootsSubscription.close();
            rootsSubscription = null;
        }
        if (reader != null) {
            try {
                reader.join(100);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
            reader = null;
        }
    }

    public boolean connected() {
        return connected;
    }

    public String context() {
        return instructions == null ? "" : instructions;
    }

    private static final long DEFAULT_TIMEOUT = 30_000L;

    public PingResponse ping() throws IOException {
        return ping(DEFAULT_TIMEOUT);
    }

    public PingResponse ping(long timeoutMillis) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        RequestId reqId = new RequestId.NumericId(id.getAndIncrement());
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(reqId, future);
        try {
            transport.send(JsonRpcCodec.toJsonObject(PingCodec.toRequest(reqId)));
        } catch (IOException e) {
            pending.remove(reqId);
            throw e;
        }
        JsonRpcMessage msg;
        try {
            msg = future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (TimeoutException e) {
            try {
                notify("notifications/cancelled", CancellationCodec.toJsonObject(new CancelledNotification(reqId, "timeout")));
            } catch (IOException ignore) {
            }
            throw new IOException("Request timed out after " + timeoutMillis + " ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        } finally {
            pending.remove(reqId);
        }
        if (msg instanceof JsonRpcResponse resp) {
            return PingCodec.toPingResponse(resp);
        }
        if (msg instanceof JsonRpcError err) {
            throw new IOException(err.error().message());
        }
        throw new IOException("Unexpected message type: " + msg.getClass().getSimpleName());
    }

    public void setLogLevel(LoggingLevel level) throws IOException {
        if (level == null) throw new IllegalArgumentException("level required");
        JsonRpcMessage msg = request("logging/setLevel",
                LoggingCodec.toJsonObject(new SetLevelRequest(level)));
        if (msg instanceof JsonRpcResponse) {
            return;
        }
        if (msg instanceof JsonRpcError err) {
            throw new IOException(err.error().message());
        }
        throw new IOException("Unexpected message type: " + msg.getClass().getSimpleName());
    }

    public JsonRpcMessage request(String method, JsonObject params) throws IOException {
        return request(method, params, DEFAULT_TIMEOUT);
    }

    public JsonRpcMessage request(String method, JsonObject params, long timeoutMillis) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        RequestId reqId = new RequestId.NumericId(id.getAndIncrement());
        ProgressToken token = parseProgressToken(params);
        if (token != null) {
            progressTracker.register(token);
            progressTokens.put(reqId, token);
        }
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(reqId, future);
        try {
            transport.send(JsonRpcCodec.toJsonObject(new JsonRpcRequest(reqId, method, params)));
        } catch (IOException e) {
            pending.remove(reqId);
            if (token != null) {
                progressTokens.remove(reqId);
                progressTracker.release(token);
            }
            throw e;
        }
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (TimeoutException e) {
            try {
                notify("notifications/cancelled", CancellationCodec.toJsonObject(new CancelledNotification(reqId, "timeout")));
            } catch (IOException ignore) {
            }
            throw new IOException("Request timed out after " + timeoutMillis + " ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        } finally {
            pending.remove(reqId);
        }
    }

    public void notify(String method, JsonObject params) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        JsonRpcNotification notification = new JsonRpcNotification(method, params);
        transport.send(JsonRpcCodec.toJsonObject(notification));
    }

    @Override
    public void close() throws IOException {
        disconnect();
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
                case JsonRpcRequest req -> {
                    JsonRpcMessage resp = handleRequest(req);
                    if (resp != null) {
                        try {
                            send(resp);
                        } catch (IOException ignore) {
                        }
                    }
                }
                case JsonRpcNotification note -> handleNotification(note);
                default -> {
                }
            }
        }
    }

    private JsonRpcMessage handleRequest(JsonRpcRequest req) {
        cancellationTracker.register(req.id());
        ProgressToken token;
        try {
            token = parseProgressToken(req.params());
            if (token != null) {
                progressTracker.register(token);
                progressTokens.put(req.id(), token);
                try {
                    sendProgress(new ProgressNotification(token, 0.0, 1.0, null));
                } catch (IOException ignore) {
                }
            }
        } catch (IllegalArgumentException e) {
            cancellationTracker.release(req.id());
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }

        boolean cancelled;
        JsonRpcMessage resp;
        try {
            resp = switch (req.method()) {
                case "sampling/createMessage" -> handleCreateMessage(req);
                case "roots/list" -> handleListRoots(req);
                case "elicitation/create" -> handleElicit(req);
                case "ping" -> handlePing(req);
                default -> new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                        "Unknown method: " + req.method(), null));
            };
        } finally {
            cancelled = cancellationTracker.isCancelled(req.id());
            cancellationTracker.release(req.id());
            ProgressToken t = progressTokens.remove(req.id());
            if (t != null) {
                progressTracker.release(t);
                if (!cancelled) {
                    try {
                        sendProgress(new ProgressNotification(t, 1.0, 1.0, null));
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return cancelled ? null : resp;
    }

    private JsonRpcMessage handleCreateMessage(JsonRpcRequest req) {
        if (sampling == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                    "Sampling not supported", null));
        }
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            CreateMessageRequest cmr = SamplingCodec.toCreateMessageRequest(params);
            CreateMessageResponse resp = sampling.createMessage(cmr);
            return new JsonRpcResponse(req.id(), SamplingCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }

    private JsonRpcMessage handleListRoots(JsonRpcRequest req) {
        if (roots == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                    "Roots not supported", null));
        }
        try {
            var list = roots.list();
            return new JsonRpcResponse(req.id(), RootsCodec.toJsonObject(list));
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }

    private JsonRpcMessage handleElicit(JsonRpcRequest req) {
        if (elicitation == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                    "Elicitation not supported", null));
        }
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            ElicitationRequest er = ElicitationCodec.toRequest(params);
            ElicitationResponse resp = elicitation.elicit(er);
            if (resp.action() == ElicitationAction.ACCEPT) {
                try {
                    SchemaValidator.validate(er.requestedSchema(), resp.content());
                } catch (IllegalArgumentException ve) {
                    return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                            JsonRpcErrorCode.INVALID_PARAMS.code(), ve.getMessage(), null));
                }
            }
            return new JsonRpcResponse(req.id(), ElicitationCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }

    private JsonRpcMessage handlePing(JsonRpcRequest req) {
        try {
            PingCodec.toPingRequest(req);
            return PingCodec.toResponse(req.id());
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    private void send(JsonRpcMessage msg) throws IOException {
        transport.send(JsonRpcCodec.toJsonObject(msg));
    }

    private void sendProgress(ProgressNotification note) throws IOException {
        try {
            progressLimiter.requireAllowance(note.token().toString());
            progressTracker.update(note);
        } catch (IllegalArgumentException | IllegalStateException ignore) {
            return;
        }
        notify("notifications/progress", ProgressCodec.toJsonObject(note));
    }

    private ProgressToken parseProgressToken(JsonObject params) {
        if (params == null || !params.containsKey("_meta")) return null;
        JsonObject meta = params.getJsonObject("_meta");
        MetaValidator.requireValid(meta);
        if (!meta.containsKey("progressToken")) return null;
        var val = meta.get("progressToken");
        return switch (val.getValueType()) {
            case STRING -> new ProgressToken.StringToken(meta.getString("progressToken"));
            case NUMBER -> new ProgressToken.NumericToken(meta.getJsonNumber("progressToken").doubleValue());
            default -> throw new IllegalArgumentException("progressToken must be a string or number");
        };
    }

    public void setProgressListener(ProgressListener listener) {
        progressListener = listener == null ? n -> {
        } : listener;
    }

    public void setLoggingListener(LoggingListener listener) {
        loggingListener = listener == null ? n -> {
        } : listener;
    }

    private void handleNotification(JsonRpcNotification note) {
        switch (note.method()) {
            case "notifications/progress" -> {
                if (note.params() != null) {
                    try {
                        ProgressNotification pn = ProgressCodec.toProgressNotification(note.params());
                        progressTracker.update(pn);
                        progressListener.onProgress(pn);
                        if (pn.progress() >= 1.0) {
                            progressTracker.release(pn.token());
                            progressTokens.values().removeIf(t -> t.equals(pn.token()));
                        }
                    } catch (IllegalArgumentException | IllegalStateException ignore) {
                    }
                }
            }
            case "notifications/message" -> {
                if (note.params() != null) {
                    try {
                        loggingListener.onMessage(LoggingCodec.toLoggingMessageNotification(note.params()));
                    } catch (IllegalArgumentException ignore) {
                    }
                }
            }
            case "notifications/cancelled" -> cancelled(note);
            default -> {
            }
        }
    }

    private void cancelled(JsonRpcNotification note) {
        CancelledNotification cn = CancellationCodec.toCancelledNotification(note.params());
        cancellationTracker.cancel(cn.requestId(), cn.reason());
        ProgressToken token = progressTokens.remove(cn.requestId());
        if (token != null) {
            progressTracker.release(token);
        }
        if (cn.reason() != null && System.err != null) {
            System.err.println("Request " + cn.requestId() + " cancelled: " + cn.reason());
        }
    }
}
