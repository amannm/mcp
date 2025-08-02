package com.amannmalik.mcp.client;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.client.elicitation.ElicitCodec;
import com.amannmalik.mcp.client.elicitation.ElicitRequest;
import com.amannmalik.mcp.client.elicitation.ElicitResult;
import com.amannmalik.mcp.client.elicitation.ElicitationAction;
import com.amannmalik.mcp.client.elicitation.ElicitationProvider;
import com.amannmalik.mcp.client.roots.RootsCodec;
import com.amannmalik.mcp.client.roots.RootsListChangedNotification;
import com.amannmalik.mcp.client.roots.RootsProvider;
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
import com.amannmalik.mcp.lifecycle.ClientFeatures;
import com.amannmalik.mcp.lifecycle.ClientInfo;
import com.amannmalik.mcp.lifecycle.InitializeRequest;
import com.amannmalik.mcp.lifecycle.InitializeResponse;
import com.amannmalik.mcp.lifecycle.LifecycleCodec;
import com.amannmalik.mcp.lifecycle.Protocol;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.lifecycle.ServerFeatures;
import com.amannmalik.mcp.lifecycle.UnsupportedProtocolVersionException;
import com.amannmalik.mcp.ping.PingCodec;
import com.amannmalik.mcp.ping.PingMonitor;
import com.amannmalik.mcp.ping.PingResponse;
import com.amannmalik.mcp.ping.PingScheduler;
import com.amannmalik.mcp.prompts.PromptsListener;
import com.amannmalik.mcp.security.RateLimiter;
import com.amannmalik.mcp.security.SamplingAccessPolicy;
import com.amannmalik.mcp.server.logging.LoggingCodec;
import com.amannmalik.mcp.server.logging.LoggingLevel;
import com.amannmalik.mcp.server.logging.LoggingListener;
import com.amannmalik.mcp.server.logging.SetLevelRequest;
import com.amannmalik.mcp.server.resources.ResourceListListener;
import com.amannmalik.mcp.server.tools.ToolCodec;
import com.amannmalik.mcp.server.tools.ToolListListener;
import com.amannmalik.mcp.transport.StreamableHttpClientTransport;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.transport.UnauthorizedException;
import com.amannmalik.mcp.transport.ResourceMetadata;
import com.amannmalik.mcp.transport.ResourceMetadataCodec;
import com.amannmalik.mcp.util.CancellationCodec;
import com.amannmalik.mcp.util.CancellationTracker;
import com.amannmalik.mcp.util.CancelledNotification;
import com.amannmalik.mcp.util.CloseUtil;
import com.amannmalik.mcp.util.ProgressCodec;
import com.amannmalik.mcp.util.ProgressListener;
import com.amannmalik.mcp.util.ProgressManager;
import com.amannmalik.mcp.util.ProgressNotification;
import com.amannmalik.mcp.util.ProgressToken;


import com.amannmalik.mcp.util.ListChangeSubscription;



import com.amannmalik.mcp.util.ProgressUtil;

import com.amannmalik.mcp.util.JsonRpcRequestProcessor;

import com.amannmalik.mcp.util.Timeouts;
import com.amannmalik.mcp.jsonrpc.RpcHandlerRegistry;
import com.amannmalik.mcp.validation.SchemaValidator;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public final class McpClient implements AutoCloseable {
    private final ClientInfo info;
    private final Set<ClientCapability> capabilities;
    private final Transport transport;
    private final SamplingProvider sampling;
    private final RootsProvider roots;
    private ListChangeSubscription rootsSubscription;
    private final boolean rootsListChangedSupported;
    private final ElicitationProvider elicitation;
    private SamplingAccessPolicy samplingAccess = SamplingAccessPolicy.PERMISSIVE;
    private Principal principal = new Principal("default", Set.of());
    private final AtomicLong id = new AtomicLong(1);
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private final CancellationTracker cancellationTracker = new CancellationTracker();
    private final ProgressManager progressManager = new ProgressManager(new RateLimiter(20, 1000));
    private final JsonRpcRequestProcessor requestProcessor;
    private Thread reader;
    private PingScheduler pinger;
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
    private ResourceListListener resourceListListener = () -> {
    };
    private ToolListListener toolListListener = () -> {
    };
    private PromptsListener promptsListener = () -> {
    };
    private volatile ResourceMetadata resourceMetadata;

    private final RpcHandlerRegistry handlers;

    public void configurePing(long intervalMillis, long timeoutMillis) {
        if (connected) throw new IllegalStateException("already connected");
        if (intervalMillis < 0 || timeoutMillis <= 0) throw new IllegalArgumentException("invalid ping settings");
        this.pingInterval = intervalMillis;
        this.pingTimeout = timeoutMillis;
    }

    public void setSamplingAccessPolicy(SamplingAccessPolicy policy) {
        samplingAccess = policy == null ? SamplingAccessPolicy.PERMISSIVE : policy;
    }

    public void setPrincipal(Principal principal) {
        if (principal != null) this.principal = principal;
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

        this.requestProcessor = new JsonRpcRequestProcessor(
                progressManager,
                cancellationTracker,
                n -> notify(n.method(), n.params()));
        this.handlers = new RpcHandlerRegistry(requestProcessor);

        handlers.register(RequestMethod.SAMPLING_CREATE_MESSAGE, this::handleCreateMessage);
        handlers.register(RequestMethod.ROOTS_LIST, this::handleListRoots);
        handlers.register(RequestMethod.ELICITATION_CREATE, this::handleElicit);
        handlers.register(RequestMethod.PING, this::handlePing);

        handlers.register(NotificationMethod.PROGRESS, this::handleProgress);
        handlers.register(NotificationMethod.MESSAGE, this::handleMessage);
        handlers.register(NotificationMethod.CANCELLED, this::cancelled);
        handlers.register(NotificationMethod.RESOURCES_LIST_CHANGED, n -> resourceListListener.listChanged());
        handlers.register(NotificationMethod.TOOLS_LIST_CHANGED, this::handleToolsListChanged);
        handlers.register(NotificationMethod.PROMPTS_LIST_CHANGED, n -> promptsListener.listChanged());
    }

    public ClientInfo info() {
        return info;
    }

    public synchronized void connect() throws IOException {
        if (connected) return;
        JsonRpcMessage msg = sendInitialization();
        handleInitialization(msg);
        notifyInitialized();
        connected = true;
        subscribeRootsIfNeeded();
        startBackgroundTasks();
    }

    private JsonRpcMessage sendInitialization() throws IOException {
        InitializeRequest init = new InitializeRequest(
                Protocol.LATEST_VERSION,
                new Capabilities(capabilities, Set.of(), Map.of(), Map.of()),
                info,
                new ClientFeatures(rootsListChangedSupported)
        );
        var initJson = LifecycleCodec.toJsonObject(init);
        RequestId reqId = new RequestId.NumericId(id.getAndIncrement());
        JsonRpcRequest request = new JsonRpcRequest(reqId, RequestMethod.INITIALIZE.method(), initJson);
        try {
            transport.send(JsonRpcCodec.toJsonObject(request));
        } catch (UnauthorizedException e) {
            handleUnauthorized(e);
            throw e;
        }
        CompletableFuture<JsonRpcMessage> future = CompletableFuture.supplyAsync(() -> {
            try {
                return JsonRpcCodec.fromJsonObject(transport.receive());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            return future.get(Timeouts.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            try {
                transport.close();
            } catch (IOException ignore) {
            }
            throw new IOException("Initialization timed out after " + Timeouts.DEFAULT_TIMEOUT_MS + " ms");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        }
    }

    private void handleInitialization(JsonRpcMessage msg) throws IOException {
        if (msg instanceof JsonRpcResponse resp) {
            InitializeResponse ir = LifecycleCodec.toInitializeResponse(resp.result());
            String serverVersion = ir.protocolVersion();
            if (!Protocol.LATEST_VERSION.equals(serverVersion) && !Protocol.PREVIOUS_VERSION.equals(serverVersion)) {
                try {
                    transport.close();
                } catch (IOException ignore) {
                }
                throw new UnsupportedProtocolVersionException(serverVersion, Protocol.LATEST_VERSION + " or " + Protocol.PREVIOUS_VERSION);
            }
            if (transport instanceof StreamableHttpClientTransport http) {
                http.setProtocolVersion(serverVersion);
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
    }

    private void notifyInitialized() throws IOException {
        JsonRpcNotification note = new JsonRpcNotification(NotificationMethod.INITIALIZED.method(), null);
        transport.send(JsonRpcCodec.toJsonObject(note));
    }

    private void subscribeRootsIfNeeded() throws IOException {
        if (roots == null || !capabilities.contains(ClientCapability.ROOTS) || !rootsListChangedSupported) return;
        try {
            rootsSubscription = roots.subscribe(() -> {
                try {
                    notify(NotificationMethod.ROOTS_LIST_CHANGED,
                            RootsCodec.toJsonObject(new RootsListChangedNotification()));
                } catch (IOException ignore) {
                }
            });
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void startBackgroundTasks() {
        reader = new Thread(this::readLoop);
        reader.setDaemon(true);
        reader.start();
        if (pingInterval > 0) {
            pinger = new PingScheduler(this, pingInterval, pingTimeout, () -> {
                try {
                    disconnect();
                } catch (IOException ignore) {
                }
            }, 3);
            pinger.start();
        }
    }

    public synchronized void disconnect() throws IOException {
        if (!connected) return;
        connected = false;
        if (pinger != null) {
            pinger.close();
            pinger = null;
        }
        transport.close();
        if (rootsSubscription != null) {
            CloseUtil.closeQuietly(rootsSubscription);
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


    public PingResponse ping() throws IOException {
        return ping(Timeouts.DEFAULT_TIMEOUT_MS);
    }

    public PingResponse ping(long timeoutMillis) throws IOException {
        JsonRpcMessage msg = request(RequestMethod.PING, null, timeoutMillis);
        if (msg instanceof JsonRpcResponse resp) return PingCodec.toPingResponse(resp);
        if (msg instanceof JsonRpcError err) throw new IOException(err.error().message());
        throw new IOException("Unexpected message type: " + msg.getClass().getSimpleName());
    }

    public void setLogLevel(LoggingLevel level) throws IOException {
        if (level == null) throw new IllegalArgumentException("level required");
        JsonRpcMessage msg = request(RequestMethod.LOGGING_SET_LEVEL,
                LoggingCodec.toJsonObject(new SetLevelRequest(level, null)));
        if (msg instanceof JsonRpcResponse) {
            return;
        }
        if (msg instanceof JsonRpcError err) {
            throw new IOException(err.error().message());
        }
        throw new IOException("Unexpected message type: " + msg.getClass().getSimpleName());
    }

    public void setAccessToken(String token) {
        if (!(transport instanceof StreamableHttpClientTransport http)) {
            throw new IllegalStateException("HTTP transport required");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token required");
        }
        http.setAuthorization(token);
    }

    public void clearAccessToken() {
        if (transport instanceof StreamableHttpClientTransport http) {
            http.clearAuthorization();
        }
    }

    public JsonRpcMessage request(String method, JsonObject params) throws IOException {
        return request(method, params, Timeouts.DEFAULT_TIMEOUT_MS);
    }

    public JsonRpcMessage request(RequestMethod method, JsonObject params) throws IOException {
        return request(method.method(), params, Timeouts.DEFAULT_TIMEOUT_MS);
    }

    public JsonRpcMessage request(RequestMethod method, JsonObject params, long timeoutMillis) throws IOException {
        requireCapability(method);
        return request(method.method(), params, timeoutMillis);
    }

    public JsonRpcMessage request(String method, JsonObject params, long timeoutMillis) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        RequestId reqId = new RequestId.NumericId(id.getAndIncrement());
        Optional<ProgressToken> token = progressManager.register(reqId, params);
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(reqId, future);
        try {
            transport.send(JsonRpcCodec.toJsonObject(new JsonRpcRequest(reqId, method, params)));
        } catch (UnauthorizedException e) {
            pending.remove(reqId);
            token.ifPresent(t -> progressManager.release(reqId));
            handleUnauthorized(e);
            throw e;
        } catch (IOException e) {
            pending.remove(reqId);
            token.ifPresent(t -> progressManager.release(reqId));
            throw e;
        }
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (TimeoutException e) {
            try {
                notify(NotificationMethod.CANCELLED, CancellationCodec.toJsonObject(new CancelledNotification(reqId, "timeout")));
            } catch (IOException ignore) {
            }
            token.ifPresent(t -> progressManager.release(reqId));
            throw new IOException("Request timed out after " + timeoutMillis + " ms");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            token.ifPresent(t -> progressManager.release(reqId));
            throw new IOException(cause);
        } finally {
            pending.remove(reqId);
            token.ifPresent(t -> progressManager.release(reqId));
        }
    }

    public void notify(String method, JsonObject params) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        JsonRpcNotification notification = new JsonRpcNotification(method, params);
        transport.send(JsonRpcCodec.toJsonObject(notification));
    }

    public void notify(NotificationMethod method, JsonObject params) throws IOException {
        notify(method.method(), params);
    }

    @Override
    public void close() throws IOException {
        disconnect();
    }

    public Set<ServerCapability> serverCapabilities() {
        return serverCapabilities;
    }

    public boolean resourcesSubscribeSupported() {
        return resourcesSubscribeSupported;
    }

    public boolean resourcesListChangedSupported() {
        return resourcesListChangedSupported;
    }

    public boolean toolsListChangedSupported() {
        return toolsListChangedSupported;
    }

    public boolean promptsListChangedSupported() {
        return promptsListChangedSupported;
    }

    public Optional<ResourceMetadata> resourceMetadata() {
        return Optional.ofNullable(resourceMetadata);
    }

    private void handleUnauthorized(UnauthorizedException e) throws IOException {
        var url = e.resourceMetadata();
        if (url.isPresent()) {
            fetchResourceMetadata(url.get());
        }
    }

    private void fetchResourceMetadata(String url) throws IOException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<InputStream> resp;
        try {
            resp = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        }
        if (resp.statusCode() != 200) {
            resp.body().close();
            throw new IOException("failed to fetch resource metadata: HTTP " + resp.statusCode());
        }
        try (InputStream body = resp.body(); JsonReader reader = Json.createReader(body)) {
            resourceMetadata = ResourceMetadataCodec.fromJsonObject(reader.readObject());
        }
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
                case JsonRpcResponse resp -> handleResponse(resp);
                case JsonRpcError err -> handleError(err);
                case JsonRpcRequest req -> handleRequest(req);
                case JsonRpcNotification note -> handleNotification(note);
                default -> {
                }
            }
        }
    }

    private void handleResponse(JsonRpcResponse resp) {
        completePending(resp.id(), resp);
    }

    private void handleError(JsonRpcError err) {
        completePending(err.id(), err);
    }

    private void handleRequest(JsonRpcRequest req) {
        Optional<JsonRpcMessage> resp;
        try {
            resp = handlers.handle(req, true);
        } catch (IOException e) {
            resp = Optional.of(JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage()));
        }
        resp.ifPresent(r -> {
            try {
                send(r);
            } catch (IOException ignore) {
            }
        });
    }

    private void handleNotification(JsonRpcNotification note) {
        try {
            handlers.handle(note);
        } catch (IOException ignore) {
        }
    }

    private void completePending(RequestId id, JsonRpcMessage msg) {
        CompletableFuture<JsonRpcMessage> f = pending.remove(id);
        if (f != null) f.complete(msg);
    }

    private JsonRpcMessage handleCreateMessage(JsonRpcRequest req) {
        if (sampling == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Sampling not supported");
        }
        JsonObject params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            CreateMessageRequest cmr = SamplingCodec.toCreateMessageRequest(params);
            try {
                samplingAccess.requireAllowed(principal);
            } catch (SecurityException e) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
            }
            CreateMessageResponse resp = sampling.createMessage(cmr);
            return new JsonRpcResponse(req.id(), SamplingCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Sampling interrupted");
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage handleListRoots(JsonRpcRequest req) {
        if (roots == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Roots not supported");
        }
        try {
            var list = roots.list();
            return new JsonRpcResponse(req.id(), RootsCodec.toJsonObject(list));
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage handleElicit(JsonRpcRequest req) {
        if (elicitation == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Elicitation not supported");
        }
        JsonObject params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            ElicitRequest er = ElicitCodec.toRequest(params);
            ElicitResult resp = elicitation.elicit(er);
            if (resp.action() == ElicitationAction.ACCEPT) {
                try {
                    SchemaValidator.validate(er.requestedSchema(), resp.content());
                } catch (IllegalArgumentException ve) {
                    return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, ve.getMessage());
                }
            }
            return new JsonRpcResponse(req.id(), ElicitCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage handlePing(JsonRpcRequest req) {
        try {
            PingCodec.toPingRequest(req);
            return PingCodec.toResponse(req.id());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private void send(JsonRpcMessage msg) throws IOException {
        transport.send(JsonRpcCodec.toJsonObject(msg));
    }

    private void requireCapability(RequestMethod method) {
        method.requiredCapability()
                .filter(c -> !serverCapabilities.contains(c))
                .ifPresent(c -> {
                    throw new IllegalStateException("Server capability not negotiated: " + c);
                });
    }

    public void setProgressListener(ProgressListener listener) {
        progressListener = listener == null ? n -> {
        } : listener;
    }

    public void setLoggingListener(LoggingListener listener) {
        loggingListener = listener == null ? n -> {
        } : listener;
    }

    public void setResourceListListener(ResourceListListener listener) {
        resourceListListener = listener == null ? () -> {
        } : listener;
    }

    public void setToolListListener(ToolListListener listener) {
        toolListListener = listener == null ? () -> {
        } : listener;
    }

    public void setPromptsListener(PromptsListener listener) {
        promptsListener = listener == null ? () -> {
        } : listener;
    }

    private void handleProgress(JsonRpcNotification note) {
        if (note.params() == null) return;
        try {
            ProgressNotification pn = ProgressCodec.toProgressNotification(note.params());
            progressManager.record(pn);
            progressListener.onProgress(pn);
        } catch (IllegalArgumentException | IllegalStateException ignore) {
        }
    }

    private void handleMessage(JsonRpcNotification note) {
        if (note.params() == null) return;
        try {
            loggingListener.onMessage(LoggingCodec.toLoggingMessageNotification(note.params()));
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void handleToolsListChanged(JsonRpcNotification note) {
        try {
            ToolCodec.toToolListChangedNotification(note.params());
            toolListListener.listChanged();
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void cancelled(JsonRpcNotification note) {
        CancelledNotification cn = CancellationCodec.toCancelledNotification(note.params());
        cancellationTracker.cancel(cn.requestId(), cn.reason());
        progressManager.release(cn.requestId());
        String reason = cancellationTracker.reason(cn.requestId());
        if (reason != null) {
            System.err.println("Request " + cn.requestId() + " cancelled: " + reason);
        }
    }

}
