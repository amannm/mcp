package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.resources.ResourceListChangedNotification;
import com.amannmalik.mcp.roots.RootsListChangedNotification;
import com.amannmalik.mcp.tools.ToolListChangedNotification;
import com.amannmalik.mcp.transport.Protocol;
import com.amannmalik.mcp.util.*;
import jakarta.json.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/// - [Overview](specification/2025-06-18/index.mdx)
/// - [Client Features](specification/2025-06-18/client/index.mdx)
/// - [Sampling](specification/2025-06-18/client/sampling.mdx)
/// - [Elicitation](specification/2025-06-18/client/elicitation.mdx)
public final class McpClient extends JsonRpcEndpoint implements AutoCloseable {
    private static final InitializeRequestAbstractEntityCodec INITIALIZE_REQUEST_CODEC = new InitializeRequestAbstractEntityCodec();
    private static final JsonCodec<ResourceUpdatedNotification> RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC = new ResourceUpdatedNotificationAbstractEntityCodec();
    private static final JsonCodec<SubscribeRequest> SUBSCRIBE_REQUEST_JSON_CODEC = new SubscribeRequestAbstractEntityCodec();
    private static final JsonCodec<UnsubscribeRequest> UNSUBSCRIBE_REQUEST_JSON_CODEC = new UnsubscribeRequestAbstractEntityCodec();
    private static final JsonCodec<SetLevelRequest> SET_LEVEL_REQUEST_JSON_CODEC = new SetLevelRequestAbstractEntityCodec();
    private static final CancelledNotificationJsonCodec CANCELLED_NOTIFICATION_JSON_CODEC = new CancelledNotificationJsonCodec();
    private static final JsonCodec<LoggingMessageNotification> LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC = new LoggingMessageNotificationAbstractEntityCodec();
    private static final JsonCodec<ProgressNotification> PROGRESS_NOTIFICATION_JSON_CODEC = new ProgressNotificationJsonCodec();
    private final ClientInfo info;
    private final Set<ClientCapability> capabilities;
    private final SamplingProvider sampling;
    private final RootsProvider roots;
    private final boolean rootsListChangedSupported;
    private final ElicitationProvider elicitation;
    private final McpClientListener listener;
    private final Map<String, Consumer<ResourceUpdate>> resourceListeners = new ConcurrentHashMap<>();
    private ChangeSubscription rootsSubscription;
    private SamplingAccessPolicy samplingAccess = SamplingAccessPolicy.PERMISSIVE;
    private Principal principal = new Principal(McpConfiguration.current().defaultPrincipal(), Set.of());
    private Thread reader;
    private ScheduledExecutorService pingExec;
    private int pingFailures;
    private long pingInterval;
    private long pingTimeout;
    private volatile boolean connected;
    private Set<ServerCapability> serverCapabilities = Set.of();
    private String instructions;
    private ServerFeatures serverFeatures = new ServerFeatures(false, false, false, false);
    private String protocolVersion;
    private ServerInfo serverInfo;
    private volatile ResourceMetadata resourceMetadata;

    McpClient(ClientInfo info,
              Set<ClientCapability> capabilities,
              Transport transport,
              SamplingProvider sampling,
              RootsProvider roots,
              ElicitationProvider elicitation,
              McpClientListener listener) {
        super(transport,
                new ProgressManager(new RateLimiter(McpConfiguration.current().progressPerSecond(), 1000)),
                1);
        this.info = info;
        this.capabilities = capabilities.isEmpty() ? Set.of() : EnumSet.copyOf(capabilities);
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
        this.pingTimeout = McpConfiguration.current().pingMs();

        registerRequest(RequestMethod.SAMPLING_CREATE_MESSAGE.method(), this::handleCreateMessage);
        registerRequest(RequestMethod.ROOTS_LIST.method(), this::handleListRoots);
        registerRequest(RequestMethod.ELICITATION_CREATE.method(), this::handleElicit);
        registerRequest(RequestMethod.PING.method(), this::handlePing);

        registerNotification(NotificationMethod.PROGRESS.method(), this::handleProgress);
        registerNotification(NotificationMethod.MESSAGE.method(), this::handleMessage);
        registerNotification(NotificationMethod.CANCELLED.method(), this::cancelled);
        registerNotification(NotificationMethod.RESOURCES_LIST_CHANGED.method(), this::handleResourcesListChanged);
        registerNotification(NotificationMethod.RESOURCES_UPDATED.method(), this::handleResourceUpdated);
        registerNotification(NotificationMethod.TOOLS_LIST_CHANGED.method(), this::handleToolsListChanged);
        if (listener != null) {
            registerNotification(NotificationMethod.PROMPTS_LIST_CHANGED.method(), n -> listener.onPromptsListChanged());
        }
    }

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

    public ClientInfo info() {
        return info;
    }

    public synchronized void connect() throws IOException {
        if (connected) return;
        JsonRpcMessage msg = sendInitialization();
        handleInitialization(msg);
        connected = true;
        try {
            transport.listen();
        } catch (UnauthorizedException e) {
            handleUnauthorized(e);
            throw e;
        }
        startBackgroundTasks();
        subscribeRootsIfNeeded();
        notifyInitialized();
    }

    public synchronized void disconnect() throws IOException {
        if (!connected) return;
        connected = false;
        if (pingExec != null) {
            pingExec.shutdownNow();
            pingExec = null;
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

    public void ping(long timeoutMillis) throws IOException {
        JsonRpcResponse resp = JsonRpc.expectResponse(request(RequestMethod.PING, null, timeoutMillis));
        if (!resp.result().isEmpty()) {
            throw new IOException("Unexpected ping response");
        }
    }

    public void setLogLevel(LoggingLevel level) throws IOException {
        if (level == null) throw new IllegalArgumentException("level required");
        JsonRpc.expectResponse(request(RequestMethod.LOGGING_SET_LEVEL,
                SET_LEVEL_REQUEST_JSON_CODEC.toJson(new SetLevelRequest(level, null)), 0L));
    }

    JsonRpcMessage request(RequestMethod method, JsonObject params, long timeoutMillis) throws IOException {
        requireCapability(method);
        if (!connected) {
            return JsonRpcError.of(new RequestId.NumericId(0), -32002, "Server not initialized");
        }
        try {
            RequestId id = nextId();
            CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
            pending.put(id, future);
            progress.register(id, params);
            send(new JsonRpcRequest(id, method.method(), params));
            try {
                return super.await(id, future, timeoutMillis);
            } finally {
                pending.remove(id);
                progress.release(id);
            }
        } catch (UnauthorizedException e) {
            handleUnauthorized(e);
            throw e;
        }
    }

    public void notify(NotificationMethod method, JsonObject params) throws IOException {
        if (!connected) throw new IllegalStateException("not connected");
        notify(method.method(), params);
    }

    @Override
    public void close() throws IOException {
        disconnect();
    }

    public String protocolVersion() {
        return protocolVersion;
    }

    public ServerInfo serverInfo() {
        return serverInfo;
    }

    public Set<ServerCapability> serverCapabilities() {
        return serverCapabilities;
    }

    public Set<String> serverCapabilityNames() {
        return serverCapabilities.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }

    public Map<String, String> serverInfoMap() {
        return Map.of(
                "name", serverInfo.name(),
                "title", serverInfo.title(),
                "version", serverInfo.version());
    }

    public ListResourcesResult listResources(String cursor) throws IOException {
        JsonRpcResponse resp = JsonRpc.expectResponse(request(
                RequestMethod.RESOURCES_LIST,
                AbstractEntityCodec.paginatedRequest(
                        ListResourcesRequest::cursor,
                        ListResourcesRequest::_meta,
                        ListResourcesRequest::new).toJson(new ListResourcesRequest(cursor, null)), 0L
        ));
        return AbstractEntityCodec.paginatedResult(
                "resources",
                "resource",
                r -> new Pagination.Page<>(r.resources(), r.nextCursor()),
                ListResourcesResult::_meta,
                new ResourceAbstractEntityCodec(),
                (page, meta) -> new ListResourcesResult(page.items(), page.nextCursor(), meta)).fromJson(resp.result());
    }

    public ListResourceTemplatesResult listResourceTemplates(String cursor) throws IOException {
        JsonRpcResponse resp = JsonRpc.expectResponse(request(
                RequestMethod.RESOURCES_TEMPLATES_LIST,
                AbstractEntityCodec.paginatedRequest(
                        ListResourceTemplatesRequest::cursor,
                        ListResourceTemplatesRequest::_meta,
                        ListResourceTemplatesRequest::new).toJson(new ListResourceTemplatesRequest(cursor, null)), 0L
        ));
        return AbstractEntityCodec.paginatedResult(
                "resourceTemplates",
                "resourceTemplate",
                r -> new Pagination.Page<>(r.resourceTemplates(), r.nextCursor()),
                ListResourceTemplatesResult::_meta,
                new ResourceTemplateAbstractEntityCodec(),
                (page1, meta) -> new ListResourceTemplatesResult(page1.items(), page1.nextCursor(), meta)).fromJson(resp.result());
    }

    public ChangeSubscription subscribeResource(String uri, Consumer<ResourceUpdate> listener) throws IOException {
        if (!serverFeatures.resourcesSubscribe()) {
            throw new IllegalStateException("resource subscribe not supported");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener required");
        }
        JsonRpc.expectResponse(request(
                RequestMethod.RESOURCES_SUBSCRIBE,
                SUBSCRIBE_REQUEST_JSON_CODEC.toJson(new SubscribeRequest(uri, null)), 0L
        ));
        resourceListeners.put(uri, listener);
        return () -> {
            resourceListeners.remove(uri);
            try {
                request(
                        RequestMethod.RESOURCES_UNSUBSCRIBE,
                        UNSUBSCRIBE_REQUEST_JSON_CODEC.toJson(new UnsubscribeRequest(uri, null)), 0L
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
            resourceMetadata = ResourceMetadata.CODEC.fromJson(reader.readObject());
        }
    }

    private JsonRpcMessage sendInitialization() throws IOException {
        InitializeRequest init = new InitializeRequest(
                Protocol.LATEST_VERSION,
                new Capabilities(capabilities, Set.of(), Map.of(), Map.of()),
                info,
                new ClientFeatures(rootsListChangedSupported)
        );
        var initJson = INITIALIZE_REQUEST_CODEC.toJson(init);
        RequestId reqId = nextId();
        JsonRpcRequest request = new JsonRpcRequest(reqId, RequestMethod.INITIALIZE.method(), initJson);
        try {
            send(request);
        } catch (UnauthorizedException e) {
            handleUnauthorized(e);
            throw e;
        }
        CompletableFuture<JsonRpcMessage> future = CompletableFuture.supplyAsync(() -> {
            try {
                return CODEC.fromJson(transport.receive());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        try {
            return future.get(McpConfiguration.current().defaultMs(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            try {
                transport.close();
            } catch (IOException ignore) {
            }
            throw new IOException("Initialization timed out after " + McpConfiguration.current().defaultMs() + " ms");
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
        InitializeResponse ir = ((JsonCodec<InitializeResponse>) new InitializeResponseAbstractEntityCodec()).fromJson(resp.result());
        String serverVersion = ir.protocolVersion();
        if (!Protocol.LATEST_VERSION.equals(serverVersion) && !Protocol.PREVIOUS_VERSION.equals(serverVersion)) {
            try {
                transport.close();
            } catch (IOException ignore) {
            }
            throw new UnsupportedProtocolVersionException(serverVersion, Protocol.LATEST_VERSION + " or " + Protocol.PREVIOUS_VERSION);
        }
        transport.setProtocolVersion(serverVersion);
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
        send(new JsonRpcNotification(NotificationMethod.INITIALIZED.method(), null));
    }

    private void subscribeRootsIfNeeded() throws IOException {
        if (roots == null || !capabilities.contains(ClientCapability.ROOTS) || !rootsListChangedSupported) return;
        try {
            rootsSubscription = roots.subscribe(ignored -> {
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
            pingExec = Executors.newSingleThreadScheduledExecutor();
            pingFailures = 0;
            pingExec.scheduleAtFixedRate(() -> {
                try {
                    ping(pingTimeout);
                    pingFailures = 0;
                } catch (IOException | RuntimeException e) {
                    pingFailures++;
                    System.err.println("Ping failure: " + e.getMessage());
                    if (pingFailures >= 3) {
                        pingFailures = 0;
                        try {
                            disconnect();
                        } catch (IOException ignore) {
                        }
                    }
                }
            }, pingInterval, pingInterval, TimeUnit.MILLISECONDS);
        }
    }

    private void readLoop() {
        while (connected) {
            try {
                JsonRpcMessage msg = CODEC.fromJson(transport.receive());
                process(msg);
            } catch (IOException e) {
                pending.values().forEach(f -> f.completeExceptionally(e));
                break;
            }
        }
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
            CreateMessageRequest cmr = new CreateMessageRequestJsonCodec().fromJson(params);
            try {
                samplingAccess.requireAllowed(principal);
            } catch (SecurityException e) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
            }
            CreateMessageResponse resp = sampling.createMessage(cmr);
            return new JsonRpcResponse(req.id(), new CreateMessageResponseAbstractEntityCodec().toJson(resp));
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
            var page = roots.list(null);
            return new JsonRpcResponse(req.id(),
                    new ListRootsResultAbstractEntityCodec().toJson(new ListRootsResult(page.items(), null)));
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
            ElicitRequest er = new ElicitRequestJsonCodec().fromJson(params);
            ElicitResult resp = elicitation.elicit(er);
            return new JsonRpcResponse(req.id(), new ElicitResultJsonCodec().toJson(resp));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage handlePing(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params != null && !params.isEmpty()) {
            try {
                ValidationUtil.requireMeta(params);
            } catch (IllegalArgumentException e) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
            }
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
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
            ProgressNotification pn = PROGRESS_NOTIFICATION_JSON_CODEC.fromJson(note.params());
            progress.record(pn);
            listener.onProgress(pn);
        } catch (IllegalArgumentException | IllegalStateException ignore) {
        }
    }

    private void handleMessage(JsonRpcNotification note) {
        if (note.params() == null) return;
        try {
            listener.onMessage(LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC.fromJson(note.params()));
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void handleResourcesListChanged(JsonRpcNotification note) {
        try {
            // TODO: understand and refactor this
            AbstractEntityCodec.empty(ResourceListChangedNotification::new).fromJson(note.params());
            listener.onResourceListChanged();
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void handleResourceUpdated(JsonRpcNotification note) {
        if (note.params() == null) return;
        try {
            ResourceUpdatedNotification run = RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC.fromJson(note.params());
            Consumer<ResourceUpdate> listener = resourceListeners.get(run.uri());
            if (listener != null) {
                listener.accept(new ResourceUpdate(run.uri(), run.title()));
            }
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void handleToolsListChanged(JsonRpcNotification note) {
        try {
            // TODO: understand and refactor this
            AbstractEntityCodec.empty(ToolListChangedNotification::new).fromJson(note.params());
            listener.onToolListChanged();
        } catch (IllegalArgumentException ignore) {
        }
    }

    private void cancelled(JsonRpcNotification note) {
        CancelledNotification cn = CANCELLED_NOTIFICATION_JSON_CODEC.fromJson(note.params());
        progress.cancel(cn.requestId(), cn.reason());
        progress.release(cn.requestId());
        String reason = progress.reason(cn.requestId());
        if (reason != null) {
            System.err.println("Request " + cn.requestId() + " cancelled: " + reason);
        }
    }

    interface McpClientListener {
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
