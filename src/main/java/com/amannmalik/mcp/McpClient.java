package com.amannmalik.mcp;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.elicitation.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.logging.*;
import com.amannmalik.mcp.ping.*;
import com.amannmalik.mcp.resources.*;
import com.amannmalik.mcp.roots.*;
import com.amannmalik.mcp.sampling.*;
import com.amannmalik.mcp.transport.*;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
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
    private Principal principal = new Principal(
            McpConfiguration.current().security().auth().defaultPrincipal(), Set.of());
    private final AtomicLong id = new AtomicLong(1);
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();
    private final CancellationTracker cancellationTracker = new CancellationTracker();
    private final ProgressManager progressManager = new ProgressManager(
            new RateLimiter(McpConfiguration.current().performance().rateLimits().progressPerSecond(), 1000));
    private Thread reader;
    private PingScheduler pinger;
    private long pingInterval;
    private long pingTimeout;
    private volatile boolean connected;
    private Set<ServerCapability> serverCapabilities = Set.of();
    private String instructions;
    private ServerFeatures serverFeatures = new ServerFeatures(false, false, false, false);
    private String protocolVersion;
    private ServerInfo serverInfo;
    private final McpClientListener listener;
    private volatile ResourceMetadata resourceMetadata;
    private final Map<String, ResourceListener> resourceListeners = new ConcurrentHashMap<>();

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
        this(info, capabilities, transport, sampling, roots, elicitation, new McpClientListener() {
        });
    }

    public McpClient(ClientInfo info,
                     Set<ClientCapability> capabilities,
                     Transport transport,
                     SamplingProvider sampling,
                     RootsProvider roots,
                     ElicitationProvider elicitation,
                     McpClientListener listener) {
        this.info = info;
        this.capabilities = capabilities.isEmpty() ? Set.of() : EnumSet.copyOf(capabilities);
        this.transport = transport;
        this.sampling = sampling;
        this.roots = roots;
        this.rootsListChangedSupported = roots != null && roots.supportsListChanged();
        this.elicitation = elicitation;
        this.listener = listener == null ? new McpClientListener() {
        } : listener;
        if (this.capabilities.contains(ClientCapability.SAMPLING) && this.sampling == null) {
            throw new IllegalArgumentException("sampling capability requires provider");
        }
        if (this.capabilities.contains(ClientCapability.ELICITATION) && this.elicitation == null) {
            throw new IllegalArgumentException("elicitation capability requires provider");
        }
        this.pingInterval = 0;
        this.pingTimeout = McpConfiguration.current().system().timeouts().pingMs();

        var requestProcessor = new JsonRpcRequestProcessor(
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
        handlers.register(NotificationMethod.RESOURCES_LIST_CHANGED, this::handleResourcesListChanged);
        handlers.register(NotificationMethod.RESOURCES_UPDATED, this::handleResourceUpdated);
        handlers.register(NotificationMethod.TOOLS_LIST_CHANGED, this::handleToolsListChanged);
        handlers.register(NotificationMethod.PROMPTS_LIST_CHANGED, n -> listener.onPromptsListChanged());
    }

    public ClientInfo info() {
        return info;
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public ServerInfo serverInfo() {
        return serverInfo;
    }

    public synchronized void connect() throws IOException {
        if (connected) return;
        JsonRpcMessage msg = sendInitialization();
        handleInitialization(msg);
        connected = true;
        if (transport instanceof StreamableHttpClientTransport http) {
            try {
                http.listen();
            } catch (UnauthorizedException e) {
                handleUnauthorized(e);
                throw e;
            }
        }
        startBackgroundTasks();
        subscribeRootsIfNeeded();
        notifyInitialized();
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
        JsonRpcResponse resp;
        try {
            resp = JsonRpc.expectResponse(msg);
        } catch (IOException e) {
            throw new IOException("Initialization failed: " + e.getMessage(), e);
        }
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
        protocolVersion = serverVersion;
        serverInfo = ir.serverInfo();
        serverCapabilities = ir.capabilities().server();
        instructions = ir.instructions();
        ServerFeatures f = ir.features();
        if (f != null) {
            serverFeatures = f;
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
                            RootsListChangedNotification.CODEC.toJson(new RootsListChangedNotification()));
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
        resourceListeners.clear();
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

    public Set<ClientCapability> capabilities() {
        return Set.copyOf(capabilities);
    }

    public String context() {
        return instructions == null ? "" : instructions;
    }

    public PingResponse ping() throws IOException {
        return ping(Timeouts.DEFAULT_TIMEOUT_MS);
    }

    public PingResponse ping(long timeoutMillis) throws IOException {
        JsonRpcResponse resp = JsonRpc.expectResponse(request(RequestMethod.PING, null, timeoutMillis));
        return PingResponse.CODEC.fromJson(resp.result());
    }

    public void setLogLevel(LoggingLevel level) throws IOException {
        if (level == null) throw new IllegalArgumentException("level required");
        JsonRpc.expectResponse(request(RequestMethod.LOGGING_SET_LEVEL,
                SetLevelRequest.CODEC.toJson(new SetLevelRequest(level, null))));
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
        var reqId = new RequestId.NumericId(id.getAndIncrement());
        progressManager.register(reqId, params);
        var future = new CompletableFuture<JsonRpcMessage>();
        pending.put(reqId, future);
        try {
            transport.send(JsonRpcCodec.toJsonObject(new JsonRpcRequest(reqId, method, params)));
            return awaitResponse(reqId, future, timeoutMillis);
        } catch (UnauthorizedException e) {
            handleUnauthorized(e);
            throw e;
        } finally {
            pending.remove(reqId);
            progressManager.release(reqId);
        }
    }

    private JsonRpcMessage awaitResponse(RequestId reqId,
                                         CompletableFuture<JsonRpcMessage> future,
                                         long timeoutMillis) throws IOException {
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (TimeoutException e) {
            try {
                notify(NotificationMethod.CANCELLED,
                        CancellationCodec.toJsonObject(new CancelledNotification(reqId, "timeout")));
            } catch (IOException ignore) {
            }
            throw new IOException("Request timed out after " + timeoutMillis + " ms", e);
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
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
        return serverFeatures.resourcesSubscribe();
    }

    public boolean resourcesListChangedSupported() {
        return serverFeatures.resourcesListChanged();
    }

    public boolean toolsListChangedSupported() {
        return serverFeatures.toolsListChanged();
    }

    public boolean promptsListChangedSupported() {
        return serverFeatures.promptsListChanged();
    }

    public Optional<ResourceMetadata> resourceMetadata() {
        return Optional.ofNullable(resourceMetadata);
    }

    public ListResourcesResult listResources(String cursor) throws IOException {
        JsonRpcResponse resp = JsonRpc.expectResponse(request(
                RequestMethod.RESOURCES_LIST,
                ResourcesCodec.toJsonObject(new ListResourcesRequest(cursor, null))
        ));
        return ResourcesCodec.toListResourcesResult(resp.result());
    }

    public ListResourceTemplatesResult listResourceTemplates(String cursor) throws IOException {
        JsonRpcResponse resp = JsonRpc.expectResponse(request(
                RequestMethod.RESOURCES_TEMPLATES_LIST,
                ResourcesCodec.toJsonObject(new ListResourceTemplatesRequest(cursor, null))
        ));
        return ResourcesCodec.toListResourceTemplatesResult(resp.result());
    }

    public ResourceSubscription subscribeResource(String uri, ResourceListener listener) throws IOException {
        if (!serverFeatures.resourcesSubscribe()) {
            throw new IllegalStateException("resource subscribe not supported");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener required");
        }
        JsonRpc.expectResponse(request(
                RequestMethod.RESOURCES_SUBSCRIBE,
                ResourcesCodec.toJsonObject(new SubscribeRequest(uri, null))
        ));
        resourceListeners.put(uri, listener);
        return () -> {
            resourceListeners.remove(uri);
            try {
                request(
                        RequestMethod.RESOURCES_UNSUBSCRIBE,
                        ResourcesCodec.toJsonObject(new UnsubscribeRequest(uri, null))
                );
            } catch (IOException ignore) {
            }
        };
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
        Optional<JsonRpcMessage> resp = handlers.handle(req, true);
        resp.ifPresent(r -> {
            try {
                send(r);
            } catch (IOException ignore) {
            }
        });
    }

    private void handleNotification(JsonRpcNotification note) {
        handlers.handle(note);
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
            CreateMessageRequest cmr = CreateMessageRequest.CODEC.fromJson(params);
            try {
                samplingAccess.requireAllowed(principal);
            } catch (SecurityException e) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
            }
            CreateMessageResponse resp = sampling.createMessage(cmr);
            return new JsonRpcResponse(req.id(), CreateMessageResponse.CODEC.toJson(resp));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (InterruptedException e) {
            Thread.interrupted();
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
            return new JsonRpcResponse(req.id(),
                    ListRootsResult.CODEC.toJson(new ListRootsResult(list, null)));
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
            return new JsonRpcResponse(req.id(), ElicitCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage handlePing(JsonRpcRequest req) {
        try {
            PingRequest.CODEC.fromJson(req.params());
            return new JsonRpcResponse(req.id(), PingResponse.CODEC.toJson(new PingResponse()));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private void send(JsonRpcMessage msg) throws IOException {
        transport.send(JsonRpcCodec.toJsonObject(msg));
    }

    private void requireCapability(RequestMethod method) {
        CapabilityRequirements.forMethod(method)
                .filter(c -> !serverCapabilities.contains(c))
                .ifPresent(c -> {
                    throw new IllegalStateException("Server capability not negotiated: " + c);
                });
    }

    private void handleProgress(JsonRpcNotification note) {
        if (note.params() == null) return;
        try {
            ProgressNotification pn = ProgressCodec.toProgressNotification(note.params());
            progressManager.record(pn);
            listener.onProgress(pn);
        } catch (IllegalArgumentException | IllegalStateException ignore) {
        }
    }

    private void handleMessage(JsonRpcNotification note) {
        if (note.params() == null) return;
        try {
            listener.onMessage(LoggingMessageNotification.CODEC.fromJson(note.params()));
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void handleResourcesListChanged(JsonRpcNotification note) {
        try {
            ResourcesCodec.requireListChangedNotification(note.params());
            listener.onResourceListChanged();
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void handleResourceUpdated(JsonRpcNotification note) {
        if (note.params() == null) return;
        try {
            ResourceUpdatedNotification run = ResourcesCodec.toResourceUpdatedNotification(note.params());
            ResourceListener listener = resourceListeners.get(run.uri());
            if (listener != null) {
                listener.updated(new ResourceUpdate(run.uri(), run.title()));
            }
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void handleToolsListChanged(JsonRpcNotification note) {
        try {
            EmptyJsonObjectCodec.requireEmpty(note.params());
            listener.onToolListChanged();
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

    public interface McpClientListener {
        default void onProgress(ProgressNotification notification) {
        }

        default void onMessage(LoggingMessageNotification notification) {
        }

        default void onResourceListChanged() {
        }

        default void onToolListChanged() {
        }

        default void onPromptsListChanged() {
        }
    }

}
