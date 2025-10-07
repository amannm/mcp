package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.Notification.*;
import com.amannmalik.mcp.api.Request.*;
import com.amannmalik.mcp.api.config.*;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.resources.ResourceListChangedNotification;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.StreamableHttpClientTransport;
import com.amannmalik.mcp.util.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.net.URI;
import java.net.http.*;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/// - [Overview](specification/2025-06-18/index.mdx)
/// - [Client Features](specification/2025-06-18/client/index.mdx)
/// - [Sampling](specification/2025-06-18/client/sampling.mdx)
/// - [Elicitation](specification/2025-06-18/client/elicitation.mdx)
public final class McpClient extends JsonRpcEndpoint implements AutoCloseable {
    private static final JsonCodec<ResourceUpdatedNotification> RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC = new ResourceUpdatedNotificationAbstractEntityCodec();
    private static final JsonCodec<ResourceListChangedNotification> RESOURCE_LIST_CHANGED_NOTIFICATION_JSON_CODEC = new ResourceListChangedNotificationJsonCodec();
    private static final JsonCodec<ToolListChangedNotification> TOOL_LIST_CHANGED_NOTIFICATION_JSON_CODEC = new ToolListChangedNotificationJsonCodec();
    private static final JsonCodec<SubscribeRequest> SUBSCRIBE_REQUEST_JSON_CODEC = new SubscribeRequestAbstractEntityCodec();
    private static final JsonCodec<UnsubscribeRequest> UNSUBSCRIBE_REQUEST_JSON_CODEC = new UnsubscribeRequestAbstractEntityCodec();
    private static final JsonCodec<SetLevelRequest> SET_LEVEL_REQUEST_JSON_CODEC = new SetLevelRequestAbstractEntityCodec();
    private static final CancelledNotificationJsonCodec CANCELLED_NOTIFICATION_JSON_CODEC = new CancelledNotificationJsonCodec();
    private static final JsonCodec<LoggingMessageNotification> LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC = new LoggingMessageNotificationAbstractEntityCodec();
    private static final JsonCodec<ProgressNotification> PROGRESS_NOTIFICATION_JSON_CODEC = new ProgressNotificationJsonCodec();
    private static final Logger LOG = PlatformLog.get(McpClient.class);
    private final McpClientConfiguration config;
    private final ClientInfo info;
    private final Set<ClientCapability> capabilities;
    private final SamplingProvider sampling;
    private final RootsProvider roots;
    private final boolean rootsListChangedSupported;
    private final ElicitationProvider elicitation;
    private final McpClientListener listener;
    private final Map<URI, Consumer<ResourceUpdate>> resourceListeners = new ConcurrentHashMap<>();
    private final Duration initializationTimeout;
    private final Duration requestTimeout;
    private final AtomicBoolean connected = new AtomicBoolean();
    private final AtomicReference<ResourceMetadata> resourceMetadata = new AtomicReference<>();
    private AutoCloseable rootsSubscription;
    private SamplingAccessPolicy samplingAccess;
    private Principal principal;
    private Thread readerThread;
    private ScheduledExecutorService pingScheduler;
    private int pingFailures;
    private Duration pingInterval;
    private Duration pingTimeout;
    private Set<ServerCapability> serverCapabilities = Set.of();
    private String instructions;
    private Set<ServerFeature> serverFeatures = EnumSet.noneOf(ServerFeature.class);
    private String protocolVersion;
    private ServerInfo serverInfo;

    public McpClient(McpClientConfiguration config,
                     boolean globalVerbose,
                     SamplingProvider sampling,
                     RootsProvider roots,
                     ElicitationProvider elicitation,
                     McpClientListener listener) throws IOException {
        super(createTransport(config, globalVerbose),
                new ProgressManager(new RateLimiter(
                        config.progressPerSecond(),
                        config.rateLimiterWindow().toMillis())),
                1);
        this.config = config;
        this.info = new ClientInfo(config.serverName(), config.serverDisplayName(), config.serverVersion());
        this.capabilities = Immutable.enumSet(config.clientCapabilities());
        this.sampling = sampling;
        this.samplingAccess = ServiceLoaders.loadSingleton(SamplingAccessPolicy.class);
        if (this.capabilities.contains(ClientCapability.SAMPLING) && this.sampling == null) {
            throw new IllegalArgumentException("sampling capability requires provider");
        }
        if (this.capabilities.contains(ClientCapability.ROOTS) && roots == null) {
            throw new IllegalArgumentException("roots capability requires provider");
        }
        this.roots = roots == null ? ignored -> {
            throw new UnsupportedOperationException("Roots not supported");
        } : roots;
        this.rootsListChangedSupported = this.capabilities.contains(ClientCapability.ROOTS) && this.roots.supportsListChanged();
        if (this.capabilities.contains(ClientCapability.ELICITATION) && elicitation == null) {
            throw new IllegalArgumentException("elicitation capability requires provider");
        }
        this.elicitation = elicitation == null ? (ignoredClient, ignoredRequest) -> {
            throw new UnsupportedOperationException("Elicitation not supported");
        } : elicitation;
        this.listener = listener == null ? new McpClientListener() {
        } : listener;
        this.principal = new Principal(config.principal(), Set.of());
        this.pingInterval = config.pingInterval();
        this.pingTimeout = config.pingTimeout();
        this.initializationTimeout = config.initializeRequestTimeout();
        this.requestTimeout = config.defaultReceiveTimeout();
        registerRequest(RequestMethod.SAMPLING_CREATE_MESSAGE, this::handleCreateMessage);
        registerRequest(RequestMethod.ROOTS_LIST, this::handleListRoots);
        registerRequest(RequestMethod.ELICITATION_CREATE, this::handleElicit);
        registerRequest(RequestMethod.PING, this::handlePing);
        registerNotification(NotificationMethod.PROGRESS, this::handleProgress);
        registerNotification(NotificationMethod.MESSAGE, this::handleMessage);
        registerNotification(NotificationMethod.CANCELLED, this::cancelled);
        registerNotification(NotificationMethod.RESOURCES_LIST_CHANGED, this::handleResourcesListChanged);
        registerNotification(NotificationMethod.RESOURCES_UPDATED, this::handleResourceUpdated);
        registerNotification(NotificationMethod.TOOLS_LIST_CHANGED, this::handleToolsListChanged);
        registerNotification(NotificationMethod.PROMPTS_LIST_CHANGED, n -> listener.onPromptsListChanged());
    }

    private static Transport createTransport(McpClientConfiguration config,
                                             boolean globalVerbose) throws IOException {
        var spec = config.commandSpec();
        if (spec != null && !spec.isBlank()) {
            if (spec.startsWith("http://") || spec.startsWith("https://")) {
                var uri = URI.create(spec);
                if (spec.startsWith("https://")) {
                    var ts = config.truststorePath().isBlank() ? null : Path.of(config.truststorePath());
                    var ks = config.keystorePath().isBlank() ? null : Path.of(config.keystorePath());
                    var pins = Set.copyOf(config.certificatePins());
                    var validate = config.certificateValidationMode() != CertificateValidationMode.PERMISSIVE;
                    return new StreamableHttpClientTransport(
                            uri,
                            config.defaultReceiveTimeout(),
                            config.defaultOriginHeader(),
                            ts,
                            config.truststorePassword().toCharArray(),
                            ks,
                            config.keystorePassword().toCharArray(),
                            validate,
                            pins,
                            config.verifyHostname());
                }
                return new StreamableHttpClientTransport(
                        uri,
                        config.defaultReceiveTimeout(),
                        config.defaultOriginHeader());
            }
            var cmds = spec.split(" ");
            var verbose = config.verbose() || globalVerbose;
            return new StdioTransport(cmds,
                    verbose ? line -> LOG.log(Logger.Level.INFO, line) : s -> {
                    },
                    config.defaultReceiveTimeout(),
                    config.processShutdownWait());
        }
        return new StdioTransport(System.in, System.out, config.defaultReceiveTimeout());
    }

    private static HandshakeResult performHandshake(RequestId id,
                                                    Transport transport,
                                                    ClientInfo info,
                                                    Set<ClientCapability> capabilities,
                                                    boolean rootsListChangedSupported,
                                                    Duration timeout) throws IOException {
        var init = new InitializeRequest(
                Protocol.LATEST_VERSION,
                new Capabilities(capabilities, Set.of(), Map.of(), Map.of()),
                info,
                new ClientFeatures(rootsListChangedSupported));
        var request = new JsonRpcRequest(id, RequestMethod.INITIALIZE.method(),
                new InitializeRequestAbstractEntityCodec().toJson(init));
        transport.send(JsonRpcEndpoint.CODEC.toJson(request));
        JsonRpcMessage msg;
        try {
            msg = JsonRpcEndpoint.CODEC.fromJson(transport.receive(timeout));
        } catch (IOException e) {
            try {
                transport.close();
            } catch (IOException e2) {
                LOG.log(Logger.Level.WARNING, "Failed to close transport after initialization failure", e2);
            }
            throw new IOException("Initialization failed: " + e.getMessage(), e);
        }
        JsonRpcResponse resp;
        try {
            resp = JsonRpc.expectResponse(msg);
        } catch (IOException e) {
            throw new IOException("Initialization failed: " + e.getMessage(), e);
        }
        var ir = new InitializeResponseAbstractEntityCodec().fromJson(resp.result());
        var serverVersion = ir.protocolVersion();
        if (!Protocol.LATEST_VERSION.equals(serverVersion) && !Protocol.PREVIOUS_VERSION.equals(serverVersion)) {
            try {
                transport.close();
            } catch (IOException e2) {
                LOG.log(Logger.Level.WARNING, "Failed to close transport after unsupported protocol", e2);
            }
            throw new UnsupportedProtocolVersionException(serverVersion,
                    Protocol.LATEST_VERSION + " or " + Protocol.PREVIOUS_VERSION);
        }
        transport.setProtocolVersion(serverVersion);
        var features = ir.features();
        var caps = ir.capabilities().server();
        return new HandshakeResult(
                serverVersion,
                ir.serverInfo(),
                caps,
                Immutable.enumSet(features),
                ir.instructions());
    }

    public ClientInfo info() {
        return info;
    }

    public synchronized void connect() throws IOException {
        if (connected.get()) {
            return;
        }
        HandshakeResult init;
        try {
            init = performHandshake(nextId(), transport, info, capabilities, rootsListChangedSupported, initializationTimeout);
        } catch (UnauthorizedException e) {
            handleUnauthorized(e);
            throw e;
        }
        protocolVersion = init.protocolVersion();
        serverInfo = init.serverInfo();
        serverCapabilities = init.capabilities();
        serverFeatures = init.features();
        instructions = init.instructions();
        connected.set(true);
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
        if (!connected.get()) {
            return;
        }
        connected.set(false);
        stopBackgroundTasks();
        transport.close();
        resourceListeners.clear();
        if (rootsSubscription != null) {
            CloseUtil.close(rootsSubscription);
            rootsSubscription = null;
        }
    }

    public boolean connected() {
        return connected.get();
    }

    public Set<ClientCapability> capabilities() {
        return Set.copyOf(capabilities);
    }

    public Set<ServerCapability> serverCapabilities() {
        return Set.copyOf(serverCapabilities);
    }

    public Set<ServerFeature> serverFeatures() {
        return Set.copyOf(serverFeatures);
    }

    public String context() {
        return instructions == null ? "" : instructions;
    }

    public void ping(Duration timeoutMillis) throws IOException {
        var resp = JsonRpc.expectResponse(request(RequestMethod.PING, null, timeoutMillis));
        if (!resp.result().isEmpty()) {
            throw new IOException("Unexpected ping response");
        }
    }

    public void setLogLevel(LoggingLevel level) throws IOException {
        if (level == null) {
            throw new IllegalArgumentException("level required");
        }
        JsonRpc.expectResponse(request(RequestMethod.LOGGING_SET_LEVEL,
                SET_LEVEL_REQUEST_JSON_CODEC.toJson(new SetLevelRequest(level, null)), requestTimeout));
    }

    public JsonRpcMessage request(RequestMethod method, JsonObject params, Duration timeoutMillis) throws IOException {
        return request(nextId(), method, params, timeoutMillis);
    }

    public JsonRpcMessage request(RequestId id, RequestMethod method, JsonObject params, Duration timeoutMillis) throws IOException {
        requireCapability(method);
        if (id instanceof RequestId.NullId) {
            throw new IllegalArgumentException("id is required");
        }
        if (params != null) {
            if (params.containsKey("progressToken")) {
                throw new IllegalArgumentException("progressToken must be in _meta");
            }
            if (params.containsKey("_meta")) {
                try {
                    ValidationUtil.requireMeta(params.getJsonObject("_meta"));
                } catch (ClassCastException e) {
                    throw new IllegalArgumentException("Invalid params", e);
                }
            }
        }
        if (!connected.get()) {
            return JsonRpcError.of(new RequestId.NumericId(0), -32002, "Server not initialized");
        }
        try {
            var future = new CompletableFuture<JsonRpcMessage>();
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

    public void sendNotification(NotificationMethod method, JsonObject params) throws IOException {
        if (!connected.get()) {
            throw new IllegalStateException("not connected");
        }
        send(new JsonRpcNotification(method.method(), params));
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

    public Set<String> serverCapabilityNames() {
        return serverCapabilities.stream().map(Enum::name).collect(Collectors.toUnmodifiableSet());
    }

    public Map<String, String> serverInfoMap() {
        return Map.of(
                "name", serverInfo.name(),
                "title", serverInfo.title(),
                "version", serverInfo.version());
    }

    public ListResourcesResult listResources(Cursor cursor) throws IOException {
        return list(
                cursor,
                RequestMethod.RESOURCES_LIST,
                token -> PaginatedRequest.CODEC.toJson(new PaginatedRequest(token, null)),
                json -> AbstractEntityCodec.paginatedResult(
                        "resources",
                        "resource",
                        r -> new Pagination.Page<>(r.resources(), r.nextCursor()),
                        ListResourcesResult::_meta,
                        new ResourceAbstractEntityCodec(),
                        (page, meta) -> new ListResourcesResult(page.items(), page.nextCursor(), meta)).fromJson(json)
        );
    }

    public ListResourceTemplatesResult listResourceTemplates(Cursor cursor) throws IOException {
        return list(
                cursor,
                RequestMethod.RESOURCES_TEMPLATES_LIST,
                token -> PaginatedRequest.CODEC.toJson(new PaginatedRequest(token, null)),
                json -> AbstractEntityCodec.paginatedResult(
                        "resourceTemplates",
                        "resourceTemplate",
                        r -> new Pagination.Page<>(r.resourceTemplates(), r.nextCursor()),
                        ListResourceTemplatesResult::_meta,
                        new ResourceTemplateAbstractEntityCodec(),
                        (page, meta) -> new ListResourceTemplatesResult(page.items(), page.nextCursor(), meta)).fromJson(json)
        );
    }

    public void configurePing(Duration intervalMillis, Duration timeoutMillis) {
        if (connected.get()) {
            throw new IllegalStateException("already connected");
        }
        this.pingInterval = ValidationUtil.requirePositive(intervalMillis, "Ping interval");
        this.pingTimeout = ValidationUtil.requirePositive(timeoutMillis, "Ping timeout");
    }

    public void setSamplingAccessPolicy(SamplingAccessPolicy policy) {
        if (policy != null) {
            this.samplingAccess = policy;
        }
    }

    public void setPrincipal(Principal principal) {
        if (principal != null) {
            this.principal = principal;
        }
    }

    private void startBackgroundTasks() {
        readerThread = new Thread(this::readLoop);
        readerThread.setDaemon(true);
        readerThread.start();
        if (pingInterval != null && pingInterval.isPositive()) {
            pingScheduler = Executors.newSingleThreadScheduledExecutor();
            pingFailures = 0;
            pingScheduler.scheduleAtFixedRate(this::pingTick,
                    pingInterval.toMillis(),
                    pingInterval.toMillis(),
                    TimeUnit.MILLISECONDS);
        }
    }

    private void stopBackgroundTasks() {
        if (pingScheduler != null) {
            pingScheduler.shutdownNow();
            pingScheduler = null;
        }
        if (readerThread != null) {
            try {
                readerThread.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.log(Logger.Level.WARNING, "Interrupted while waiting for reader", e);
            }
            readerThread = null;
        }
    }

    private void readLoop() {
        while (connected()) {
            try {
                var msg = JsonRpcEndpoint.CODEC.fromJson(transport.receive());
                process(msg);
            } catch (IOException e) {
                pending.values().forEach(f -> f.completeExceptionally(e));
                break;
            }
        }
    }

    private void pingTick() {
        try {
            ping(pingTimeout);
            pingFailures = 0;
        } catch (IOException | RuntimeException e) {
            handlePingFailure(e);
        }
    }

    private void handlePingFailure(Exception e) {
        pingFailures++;
        LOG.log(Logger.Level.WARNING, "Ping failure", e);
        if (pingFailures >= 3) {
            pingFailures = 0;
            disconnectAfterPingFailures();
        }
    }

    private void disconnectAfterPingFailures() {
        try {
            disconnect();
        } catch (IOException e) {
            LOG.log(Logger.Level.ERROR, "Disconnect failed", e);
        }
    }

    private <T> T list(
            Cursor cursor,
            RequestMethod method,
            Function<String, JsonObject> requestJson,
            Function<JsonObject, T> resultParser) throws IOException {
        var token = cursor instanceof Cursor.Token(var value) ? value : null;
        var params = requestJson.apply(token);
        var resp = JsonRpc.expectResponse(request(method, params, requestTimeout));
        return resultParser.apply(resp.result());
    }

    public AutoCloseable subscribeResource(URI uri, Consumer<ResourceUpdate> listener) throws IOException {
        if (!serverFeatures.contains(ServerFeature.RESOURCES_SUBSCRIBE)) {
            throw new IllegalStateException("resource subscribe not supported");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener required");
        }
        JsonRpc.expectResponse(request(
                RequestMethod.RESOURCES_SUBSCRIBE,
                SUBSCRIBE_REQUEST_JSON_CODEC.toJson(new SubscribeRequest(uri, null)), requestTimeout
        ));
        resourceListeners.put(uri, listener);
        return () -> {
            resourceListeners.remove(uri);
            try {
                request(
                        RequestMethod.RESOURCES_UNSUBSCRIBE,
                        UNSUBSCRIBE_REQUEST_JSON_CODEC.toJson(new UnsubscribeRequest(uri, null)), requestTimeout
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
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
        var uri = URI.create(url);
        HttpClient client = httpClientFor(url, uri);
        var req = HttpRequest.newBuilder(uri)
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<InputStream> resp;
        try {
            resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IOException(ex);
        }
        if (resp.statusCode() != 200) {
            resp.body().close();
            throw new IOException("failed to fetch resource metadata: HTTP " + resp.statusCode());
        }
        try (var body = resp.body(); var reader = Json.createReader(body)) {
            resourceMetadata.set(new ResourceMetadataJsonCodec().fromJson(reader.readObject()));
        }
    }

    private HttpClient httpClientFor(String url, URI uri) {
        if (url.startsWith("https://")) {
            var ts = config.truststorePath().isBlank() ? null : Path.of(config.truststorePath());
            var ks = config.keystorePath().isBlank() ? null : Path.of(config.keystorePath());
            var pins = Set.copyOf(config.certificatePins());
            var validate = config.certificateValidationMode() != CertificateValidationMode.PERMISSIVE;
            return StreamableHttpClientTransport.buildClient(
                    uri,
                    ts,
                    config.truststorePassword().toCharArray(),
                    ks,
                    config.keystorePassword().toCharArray(),
                    validate,
                    pins,
                    config.verifyHostname());
        }
        return HttpClient.newHttpClient();
    }

    private void notifyInitialized() throws IOException {
        send(new JsonRpcNotification(NotificationMethod.INITIALIZED.method(), null));
    }

    private void subscribeRootsIfNeeded() throws IOException {
        if (!capabilities.contains(ClientCapability.ROOTS) || !rootsListChangedSupported) {
            return;
        }
        try {
            rootsSubscription = roots.onListChanged(() -> {
                try {
                    var params = AbstractEntityCodec.empty(RootsListChangedNotification::new).toJson(new RootsListChangedNotification());
                    send(new JsonRpcNotification(NotificationMethod.ROOTS_LIST_CHANGED.method(), params));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private JsonRpcMessage handleCreateMessage(JsonRpcRequest req) {
        if (sampling == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Sampling not supported");
        }
        var params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            var cmr = new CreateMessageRequestJsonCodec().fromJson(params);
            try {
                samplingAccess.requireAllowed(principal);
            } catch (SecurityException e) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
            }
            var resp = sampling.createMessage(cmr);
            return new JsonRpcResponse(req.id(), new CreateMessageResponseAbstractEntityCodec().toJson(resp));
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
        if (!capabilities.contains(ClientCapability.ROOTS)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Roots not supported");
        }
        try {
            var page = roots.list(Cursor.Start.INSTANCE);
            return new JsonRpcResponse(req.id(),
                    new ListRootsResultAbstractEntityCodec().toJson(new ListRootsResult(page.items(), null)));
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage handleElicit(JsonRpcRequest req) {
        if (!capabilities.contains(ClientCapability.ELICITATION)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Elicitation not supported");
        }
        var params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            var er = new ElicitRequestJsonCodec().fromJson(params);
            var resp = elicitation.elicit(er);
            return new JsonRpcResponse(req.id(), new ElicitResultJsonCodec().toJson(resp));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage handlePing(JsonRpcRequest req) {
        var params = req.params();
        if (params != null) {
            if (params.size() != 1 || !params.containsKey("_meta")) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Invalid params");
            }
            try {
                ValidationUtil.requireMeta(params.getJsonObject("_meta"));
            } catch (IllegalArgumentException | ClassCastException e) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
            }
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    private void requireCapability(RequestMethod method) {
        for (var cap : method.serverCapabilities()) {
            if (!serverCapabilities.contains(cap)) {
                throw new IllegalStateException("Server capability not negotiated: " + cap);
            }
        }
    }

    private void handleProgress(JsonRpcNotification note) {
        if (note.params() == null) {
            return;
        }
        var pn = PROGRESS_NOTIFICATION_JSON_CODEC.fromJson(note.params());
        progress.record(pn);
        listener.onProgress(pn);
    }

    private void handleMessage(JsonRpcNotification note) {
        if (note.params() == null) {
            return;
        }
        listener.onMessage(LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC.fromJson(note.params()));
    }

    private void handleResourcesListChanged(JsonRpcNotification note) {
        RESOURCE_LIST_CHANGED_NOTIFICATION_JSON_CODEC.fromJson(note.params());
        listener.onResourceListChanged();
    }

    private void handleResourceUpdated(JsonRpcNotification note) {
        if (note.params() == null) {
            return;
        }
        var run = RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC.fromJson(note.params());
        var listener = resourceListeners.get(run.uri());
        if (listener != null) {
            listener.accept(new ResourceUpdate(run.uri(), run.title()));
        }
    }

    private void handleToolsListChanged(JsonRpcNotification note) {
        TOOL_LIST_CHANGED_NOTIFICATION_JSON_CODEC.fromJson(note.params());
        listener.onToolListChanged();
    }

    private void cancelled(JsonRpcNotification note) {
        var cn = CANCELLED_NOTIFICATION_JSON_CODEC.fromJson(note.params());
        var reason = progress.cancel(cn.requestId(), cn.reason());
        progress.release(cn.requestId());
        reason.ifPresent(value ->
                LOG.log(Logger.Level.INFO, () -> "Request " + cn.requestId() + " cancelled: " + value));
    }

    private record HandshakeResult(String protocolVersion,
                                   ServerInfo serverInfo,
                                   Set<ServerCapability> capabilities,
                                   Set<ServerFeature> features,
                                   String instructions) {
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
