package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.api.Notification.*;
import com.amannmalik.mcp.api.Request.*;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.transport.*;
import com.amannmalik.mcp.util.*;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;

import java.io.*;
import java.lang.System.Logger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/// - [Server](specification/2025-06-18/server/index.mdx)
/// - [MCP server conformance test](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:6-34)
public final class ServerRuntime extends JsonRpcEndpoint implements McpServer {
    private static final InitializeRequestAbstractEntityCodec INITIALIZE_REQUEST_CODEC = new InitializeRequestAbstractEntityCodec();
    private static final JsonCodec<LoggingMessageNotification> LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC = new LoggingMessageNotificationAbstractEntityCodec();
    private static final CompleteRequestJsonCodec COMPLETE_REQUEST_JSON_CODEC = new CompleteRequestJsonCodec();
    private static final JsonCodec<SetLevelRequest> SET_LEVEL_REQUEST_JSON_CODEC = new SetLevelRequestAbstractEntityCodec();
    private static final CancelledNotificationJsonCodec CANCELLED_NOTIFICATION_JSON_CODEC = new CancelledNotificationJsonCodec();
    private static final JsonCodec<ToolListChangedNotification> TOOL_LIST_CHANGED_NOTIFICATION_JSON_CODEC = new ToolListChangedNotificationJsonCodec();
    private static final InitializeResponseAbstractEntityCodec INITIALIZE_RESPONSE_CODEC = new InitializeResponseAbstractEntityCodec();
    private static final JsonCodec<GetPromptRequest> GET_PROMPT_REQUEST_JSON_CODEC = new GetPromptRequestAbstractEntityCodec();
    private static final JsonCodec<PromptInstance> PROMPT_INSTANCE_JSON_CODEC = new PromptInstanceAbstractEntityCodec();
    private static final JsonCodec<ListToolsResult> LIST_TOOLS_RESULT_JSON_CODEC =
            AbstractEntityCodec.paginatedResult(
                    "tools",
                    "tool",
                    r -> new Pagination.Page<>(r.tools(), r.nextCursor()),
                    ListToolsResult::_meta,
                    new ToolAbstractEntityCodec(),
                    (page, meta) -> new ListToolsResult(page.items(), page.nextCursor(), meta));

    private static final JsonCodec<ListPromptsResult> LIST_PROMPTS_RESULT_CODEC =
            AbstractEntityCodec.paginatedResult(
                    "prompts",
                    "prompt",
                    r -> new Pagination.Page<>(r.prompts(), r.nextCursor()),
                    ListPromptsResult::_meta,
                    new PromptAbstractEntityCodec(),
                    (page, meta) -> new ListPromptsResult(page.items(), page.nextCursor(), meta));
    private static final Logger LOG = PlatformLog.get(ServerRuntime.class);
    private static final JsonCodec<ResourceListChangedNotification> RESOURCE_LIST_CHANGED_NOTIFICATION_JSON_CODEC = new ResourceListChangedNotificationJsonCodec();
    private static final JsonCodec<ResourceUpdatedNotification> RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC = new ResourceUpdatedNotificationAbstractEntityCodec();
    private static final CallToolRequestAbstractEntityCodec CALL_TOOL_REQUEST_CODEC = new CallToolRequestAbstractEntityCodec();
    private static final JsonCodec<ToolResult> TOOL_RESULT_CODEC = new ToolResultAbstractEntityCodec();
    private static final JsonCodec<PaginatedRequest> PAGINATED_REQUEST_CODEC =
            AbstractEntityCodec.paginatedRequest(PaginatedRequest::cursor, PaginatedRequest::_meta, PaginatedRequest::new);
    private final McpServerConfiguration config;
    private final Set<ServerCapability> serverCapabilities;
    private final ResourceProvider resources;
    private final ResourceAccessPolicy resourceAccess;
    private final Map<URI, AutoCloseable> resourceSubscriptions = new ConcurrentHashMap<>();
    private final ToolProvider tools;
    private final PromptProvider prompts;
    private final CompletionProvider completions;
    private final SamplingProvider sampling;
    private final RateLimiter toolLimiter;
    private final RootsManager rootsManager;
    private final ToolAccessPolicy toolAccessPolicy;
    private final SamplingAccessPolicy samplingAccess;
    private final Principal principal;
    private final RateLimiter completionLimiter;
    private final RateLimiter logLimiter;
    private final ServerLifecycle lifecycle;
    private final AtomicReference<LoggingLevel> logLevel = new AtomicReference<>();
    private AutoCloseable resourceListSubscription;
    private AutoCloseable toolListSubscription;
    private AutoCloseable promptsSubscription;
    private Runnable toolListChangedEmitter;
    private Runnable promptsListChangedEmitter;
    private Runnable resourceListChangedEmitter;

    public ServerRuntime(McpServerConfiguration config,
                         ResourceProvider resources,
                         ToolProvider tools,
                         PromptProvider prompts,
                         CompletionProvider completions,
                         SamplingProvider sampling,
                         ResourceAccessPolicy resourceAccess,
                         ToolAccessPolicy toolAccessPolicy,
                         SamplingAccessPolicy samplingAccessPolicy,
                         Principal principal,
                         String instructions) throws Exception {
        super(createTransport(config),
                new ProgressManager(new RateLimiter(config.progressPerSecond(),
                        config.rateLimiterWindowMs())),
                config.initialRequestId());
        this.config = config;
        this.completionLimiter = limiter(
                config.completionsPerSecond(),
                config.rateLimiterWindowMs());
        this.logLimiter = limiter(
                config.logsPerSecond(),
                config.rateLimiterWindowMs());
        this.serverCapabilities = capabilities(resources, tools, prompts, completions);
        var serverInfo = new ServerInfo(
                config.serverName(),
                config.serverDescription(),
                config.serverVersion());
        this.tools = tools;
        this.prompts = prompts;
        this.completions = completions;
        this.sampling = sampling;
        this.principal = principal;
        this.lifecycle = new ServerLifecycle(config.supportedVersions(), serverCapabilities, serverInfo, instructions);
        this.toolLimiter = limiter(config.toolsPerSecond(), config.rateLimiterWindowMs());
        this.toolAccessPolicy = toolAccessPolicy == null
                ? ServiceLoaders.loadSingleton(ToolAccessPolicy.class)
                : toolAccessPolicy;
        this.samplingAccess = samplingAccessPolicy == null
                ? ServiceLoaders.loadSingleton(SamplingAccessPolicy.class)
                : samplingAccessPolicy;
        this.logLevel.set(config.initialLogLevel());
        this.rootsManager = new RootsManager(lifecycle::clientCapabilities, this::request);
        this.resources = resources;
        this.resourceAccess = resourceAccess;
        subscribeListChanges(tools, prompts);
        subscribeResourceListChanges(resources);
        registerHandlers(resources, tools, prompts, completions);
    }

    private static RateLimiter limiter(int perSecond, long windowMs) {
        return new RateLimiter(perSecond, windowMs);
    }

    private static Set<ServerCapability> capabilities(ResourceProvider resources,
                                                      ToolProvider tools,
                                                      PromptProvider prompts,
                                                      CompletionProvider completions) {
        var caps = EnumSet.noneOf(ServerCapability.class);
        if (resources != null) {
            caps.add(ServerCapability.RESOURCES);
        }
        if (tools != null) {
            caps.add(ServerCapability.TOOLS);
        }
        if (prompts != null) {
            caps.add(ServerCapability.PROMPTS);
        }
        if (completions != null) {
            caps.add(ServerCapability.COMPLETIONS);
        }
        caps.add(ServerCapability.LOGGING);
        return Immutable.enumSet(caps);
    }

    private static Transport createTransport(McpServerConfiguration config) throws Exception {
        return switch (config.transportType()) {
            case "stdio" -> new StdioTransport(System.in, System.out, config.defaultTimeoutMs());
            case "http" -> {
                if (!config.insecure() && config.authServers().isEmpty()) {
                    throw new IllegalArgumentException("auth server must be specified");
                }
                AuthorizationManager authManager = null;
                if (!config.expectedAudience().isBlank()) {
                    var secret = config.jwtSecret();
                    var tokenValidator = (secret == null || secret.isBlank())
                            ? new JwtTokenValidator(config.expectedAudience())
                            : new JwtTokenValidator(config.expectedAudience(), secret.getBytes(StandardCharsets.UTF_8));
                    authManager = new AuthorizationManager(List.of(new BearerTokenAuthorizationStrategy(tokenValidator)));
                }
                var ht = new StreamableHttpServerTransport(
                        config,
                        authManager);
                if (config.verbose()) {
                    if (config.serverPort() > 0) {
                        LOG.log(Logger.Level.INFO, "Listening on http://127.0.0.1:" + ht.port());
                    }
                    if (config.httpsPort() > 0) {
                        LOG.log(Logger.Level.INFO, "Listening on https://127.0.0.1:" + ht.httpsPort());
                    }
                }
                yield ht;
            }
            default -> throw new IllegalArgumentException("Unknown transport type: " + config.transportType());
        };
    }

    private static Logger.Level level(LoggingLevel l) {
        return PlatformLog.toPlatformLevel(l);
    }

    private static <S extends AutoCloseable> S subscribeListChanges0(
            Supplier<LifecycleState> state,
            Function<Runnable, S> factory,
            Runnable listener) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(factory, "factory");
        Objects.requireNonNull(listener, "listener");
        return factory.apply(() -> {
            if (state.get() != LifecycleState.OPERATION) {
                return;
            }
            listener.run();
        });
    }

    @Override
    public void serve() throws IOException {
        while (lifecycle.state() != LifecycleState.SHUTDOWN) {
            var obj = receiveMessage();
            if (obj.isEmpty()) {
                continue;
            }
            try {
                process(CODEC.fromJson(obj.get()));
            } catch (IllegalArgumentException e) {
                handleInvalidRequest(e);
            } catch (IOException e) {
                LOG.log(Logger.Level.ERROR, () -> config.errorProcessing() + ": " + e.getMessage());
                var data = normalizeLogData(Json.createValue(e.getMessage()));
                sendLog(new LoggingMessageNotification(LoggingLevel.ERROR, config.serverLoggerName(), data));
            } catch (Exception e) {
                LOG.log(Logger.Level.ERROR, () -> "Unexpected " + config.errorProcessing().toLowerCase(Locale.ROOT) + ": " + e.getMessage());
                var data = normalizeLogData(Json.createValue(e.getMessage()));
                sendLog(new LoggingMessageNotification(LoggingLevel.ERROR, config.serverLoggerName(), data));
            }
        }
    }

    private void subscribeListChanges(ToolProvider tools, PromptProvider prompts) {
        if (tools != null && tools.supportsListChanged()) {
            toolListChangedEmitter = () -> {
                try {
                    send(new JsonRpcNotification(
                            NotificationMethod.TOOLS_LIST_CHANGED.method(),
                            TOOL_LIST_CHANGED_NOTIFICATION_JSON_CODEC.toJson(new ToolListChangedNotification())));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            toolListSubscription = subscribeListChanges0(
                    lifecycle::state,
                    tools::onListChanged,
                    toolListChangedEmitter);
        }
        if (prompts != null && prompts.supportsListChanged()) {
            promptsListChangedEmitter = () -> {
                try {
                    send(new JsonRpcNotification(
                            NotificationMethod.PROMPTS_LIST_CHANGED.method(),
                            PromptListChangedNotification.CODEC.toJson(new PromptListChangedNotification())));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            promptsSubscription = subscribeListChanges0(
                    lifecycle::state,
                    prompts::onListChanged,
                    promptsListChangedEmitter);
        }
    }

    private void subscribeResourceListChanges(ResourceProvider resources) {
        if (resources != null && resources.supportsListChanged()) {
            resourceListChangedEmitter = () -> {
                try {
                    send(new JsonRpcNotification(
                            NotificationMethod.RESOURCES_LIST_CHANGED.method(),
                            RESOURCE_LIST_CHANGED_NOTIFICATION_JSON_CODEC.toJson(new ResourceListChangedNotification())));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
            resourceListSubscription = subscribeListChanges0(
                    lifecycle::state,
                    resources::onListChanged,
                    resourceListChangedEmitter);
        }
    }

    private void registerHandlers(ResourceProvider resources,
                                  ToolProvider tools,
                                  PromptProvider prompts,
                                  CompletionProvider completions) {
        registerRequest(RequestMethod.INITIALIZE, this::initialize);
        registerNotification(NotificationMethod.INITIALIZED, this::initialized);
        registerRequest(RequestMethod.PING, this::ping);
        registerNotification(NotificationMethod.CANCELLED, this::cancelled);
        registerNotification(NotificationMethod.ROOTS_LIST_CHANGED, n -> rootsManager.listChangedNotification());
        if (resources != null) {
            registerRequest(RequestMethod.RESOURCES_LIST, this::listResources);
            registerRequest(RequestMethod.RESOURCES_READ, this::readResource);
            registerRequest(RequestMethod.RESOURCES_TEMPLATES_LIST, this::listResourceTemplates);
            if (resources.supportsSubscribe()) {
                registerRequest(RequestMethod.RESOURCES_SUBSCRIBE, this::subscribeResource);
                registerRequest(RequestMethod.RESOURCES_UNSUBSCRIBE, this::unsubscribeResource);
            }
        }
        if (tools != null) {
            registerRequest(RequestMethod.TOOLS_LIST, this::listTools);
            registerRequest(RequestMethod.TOOLS_CALL, this::callTool);
        }
        if (prompts != null) {
            registerRequest(RequestMethod.PROMPTS_LIST, this::listPrompts);
            registerRequest(RequestMethod.PROMPTS_GET, this::getPrompt);
        }
        registerRequest(RequestMethod.LOGGING_SET_LEVEL, this::setLogLevel);
        if (completions != null) {
            registerRequest(RequestMethod.COMPLETION_COMPLETE, this::complete);
        }
        registerRequest(RequestMethod.SAMPLING_CREATE_MESSAGE, this::handleCreateMessage);
    }

    private Optional<JsonObject> receiveMessage() {
        try {
            return Optional.of(transport.receive());
        } catch (EOFException e) {
            lifecycle.shutdown();
        } catch (JsonParsingException e) {
            handleParseError(e);
        } catch (IOException e) {
            LOG.log(Logger.Level.ERROR, () -> config.errorProcessing() + ": " + e.getMessage());
            try {
                var data = normalizeLogData(Json.createValue(e.getMessage()));
                sendLog(new LoggingMessageNotification(LoggingLevel.ERROR, config.serverLoggerName(), data));
            } catch (IOException ioe) {
                LOG.log(Logger.Level.ERROR, () -> "Failed to send error: " + ioe.getMessage());
            }
        }
        return Optional.empty();
    }

    private void logAndRespond(String prefix,
                               LoggingLevel logLevel,
                               String logger,
                               JsonRpcErrorCode code,
                               RequestId id,
                               String message) {
        LOG.log(level(logLevel), prefix + ": " + message);
        try {
            sendLog(new LoggingMessageNotification(logLevel, logger, normalizeLogData(Json.createValue(message))));
            send(JsonRpcError.of(id, code, message));
        } catch (IOException ioe) {
            LOG.log(Logger.Level.ERROR, () -> "Failed to send error: " + ioe.getMessage());
        }
    }

    private void emitInitialListChangedNotifications() {
        emitListChanged(toolListChangedEmitter);
        emitListChanged(promptsListChangedEmitter);
        emitListChanged(resourceListChangedEmitter);
    }

    private void emitListChanged(Runnable emitter) {
        if (emitter == null) {
            return;
        }
        try {
            emitter.run();
        } catch (UncheckedIOException e) {
            LOG.log(Logger.Level.WARNING, () -> "Failed to send list changed notification: " + e.getMessage());
        }
    }

    private void scheduleResourceSnapshot(URI uri) {
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                if (!resourceSubscriptions.containsKey(uri)) {
                    return;
                }
                var title = resources.get(uri).map(Resource::title).orElse(null);
                send(new JsonRpcNotification(
                        NotificationMethod.RESOURCES_UPDATED.method(),
                        RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC.toJson(new ResourceUpdatedNotification(uri, title))));
            } catch (IOException e) {
                LOG.log(Logger.Level.WARNING, () -> "Failed to send initial resource update: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private JsonValue normalizeLogData(JsonValue value) {
        if (value instanceof JsonObject) {
            return value;
        }
        if (value instanceof JsonString js) {
            return Json.createObjectBuilder().add("message", js.getString()).build();
        }
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            return Json.createObjectBuilder().build();
        }
        return Json.createObjectBuilder().add("value", value.toString()).build();
    }

    private void handleParseError(JsonParsingException e) {
        logAndRespond(
                config.errorParse(),
                LoggingLevel.ERROR,
                config.parserLoggerName(),
                JsonRpcErrorCode.PARSE_ERROR,
                RequestId.NullId.INSTANCE,
                e.getMessage());
    }

    private void handleInvalidRequest(IllegalArgumentException e) {
        logAndRespond(
                config.errorInvalidRequest(),
                LoggingLevel.WARNING,
                config.serverLoggerName(),
                JsonRpcErrorCode.INVALID_REQUEST,
                RequestId.NullId.INSTANCE,
                e.getMessage());
    }

    private JsonRpcMessage initialize(JsonRpcRequest req) {
        var init = INITIALIZE_REQUEST_CODEC.fromJson(req.params());
        var resp = lifecycle.initialize(init, serverFeatures());
        var json = INITIALIZE_RESPONSE_CODEC.toJson(resp);
        return new JsonRpcResponse(req.id(), json);
    }

    private void initialized(JsonRpcNotification ignored) {
        lifecycle.confirmInitialized();
        emitInitialListChangedNotifications();
        rootsManager.refreshAsync();
    }

    private JsonRpcMessage ping(JsonRpcRequest req) {
        var params = req.params();
        if (params != null) {
            if (params.size() != 1 || !params.containsKey("_meta")
                    || !(params.get("_meta") instanceof JsonObject)) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Invalid params");
            }
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    private Set<ServerFeature> serverFeatures() {
        var f = EnumSet.noneOf(ServerFeature.class);
        if (resources != null && resources.supportsSubscribe()) {
            f.add(ServerFeature.RESOURCES_SUBSCRIBE);
        }
        if (resources != null && resources.supportsListChanged()) {
            f.add(ServerFeature.RESOURCES_LIST_CHANGED);
        }
        if (tools != null && tools.supportsListChanged()) {
            f.add(ServerFeature.TOOLS_LIST_CHANGED);
        }
        if (prompts != null && prompts.supportsListChanged()) {
            f.add(ServerFeature.PROMPTS_LIST_CHANGED);
        }
        return Immutable.enumSet(f);
    }

    private void requireServerCapability(ServerCapability cap) {
        if (!serverCapabilities.contains(cap)) {
            throw new IllegalStateException("Server capability not declared: " + cap);
        }
    }

    private Duration normalizeTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            return config.defaultTimeoutMs();
        }
        return timeout;
    }

    private Optional<String> rateLimit(RateLimiter limiter, String key) {
        try {
            limiter.requireAllowance(key);
            return Optional.empty();
        } catch (SecurityException e) {
            return Optional.of(e.getMessage());
        }
    }

    private void cancelled(JsonRpcNotification note) {
        var cn = CANCELLED_NOTIFICATION_JSON_CODEC.fromJson(note.params());
        var reason = progress.cancel(cn.requestId(), cn.reason());
        progress.release(cn.requestId());
        try {
            var payload = reason.<JsonValue>map(Json::createValue).orElse(JsonValue.NULL);
            sendLog(new LoggingMessageNotification(LoggingLevel.INFO, config.cancellationLoggerName(), normalizeLogData(payload)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        var initCheck = lifecycle.ensureInitialized(req.id(), config.errorNotInitialized()).map(error -> (JsonRpcMessage) error);
        if (initCheck.isPresent()) {
            return initCheck.get();
        }
        requireServerCapability(ServerCapability.TOOLS);
        try {
            var pageReq = PAGINATED_REQUEST_CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(pageReq.cursor());
            var page = tools.list(cursor);
            var json = LIST_TOOLS_RESULT_JSON_CODEC.toJson(new ListToolsResult(page.items(), page.nextCursor(), null));
            return new JsonRpcResponse(req.id(), json);
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage invokeTool(JsonRpcRequest req, Tool tool, JsonObject args) {
        try {
            var result = tools.call(tool.name(), args);
            return new JsonRpcResponse(req.id(), TOOL_RESULT_CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return recoverTool(req, tool, e);
        }
    }

    private JsonRpcMessage recoverTool(JsonRpcRequest req, Tool tool, IllegalArgumentException failure) {
        if (!lifecycle.clientCapabilities().contains(ClientCapability.ELICITATION)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, failure.getMessage());
        }
        try {
            var er = new ElicitRequest(
                    "Provide arguments for tool '" + tool.name() + "'",
                    tool.inputSchema(),
                    null);
            var res = elicit(er);
            if (res.action() != ElicitationAction.ACCEPT) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Tool invocation cancelled");
            }
            var result = tools.call(tool.name(), res.content());
            return new JsonRpcResponse(req.id(), TOOL_RESULT_CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage callTool(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.TOOLS);
        CallToolRequest callRequest;
        try {
            callRequest = CALL_TOOL_REQUEST_CODEC.fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        var limit = rateLimit(toolLimiter, callRequest.name());
        if (limit.isPresent()) {
            return JsonRpcError.of(req.id(), config.rateLimitErrorCode(), limit.get());
        }
        var tool = tools.find(callRequest.name()).orElse(null);
        if (tool == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Unknown tool: " + callRequest.name());
        }
        try {
            toolAccessPolicy.requireAllowed(principal, tool);
        } catch (SecurityException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, config.errorAccessDenied());
        }
        return invokeTool(req, tool, callRequest.arguments());
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        var initCheck = lifecycle.ensureInitialized(req.id(), config.errorNotInitialized()).map(error -> (JsonRpcMessage) error);
        if (initCheck.isPresent()) {
            return initCheck.get();
        }
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            var pageReq = PAGINATED_REQUEST_CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(pageReq.cursor());
            var page = prompts.list(cursor);
            return new JsonRpcResponse(req.id(), LIST_PROMPTS_RESULT_CODEC.toJson(new ListPromptsResult(page.items(), page.nextCursor(), null)));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        var initCheck = lifecycle.ensureInitialized(req.id(), config.errorNotInitialized())
                .map(error -> (JsonRpcMessage) error);
        if (initCheck.isPresent()) {
            return initCheck.get();
        }
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            var getRequest = GET_PROMPT_REQUEST_JSON_CODEC.fromJson(req.params());
            var inst = prompts.get(getRequest.name(), getRequest.arguments());
            return new JsonRpcResponse(req.id(), PROMPT_INSTANCE_JSON_CODEC.toJson(inst));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        if (lifecycle.state() != LifecycleState.OPERATION) {
            return JsonRpcError.of(req.id(), -32002, "Server not initialized");
        }
        var progressToken = ProgressToken.fromMeta(req.params());
        try {
            var pageReq = PAGINATED_REQUEST_CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(pageReq.cursor());
            progressToken.ifPresent(t -> sendResourceProgress(t, 0.0, "Starting resource list"));
            var page = resources.list(cursor);
            progressToken.ifPresent(t -> sendResourceProgress(t, 0.5, "Filtering resources"));
            var filtered = page.items().stream()
                    .filter(r -> resourceAllowed(r.annotations()) && RootChecker.withinRoots(r.uri(), rootsManager.roots()))
                    .toList();
            progressToken.ifPresent(t -> sendResourceProgress(t, 1.0, "Completed resource list"));
            var result = new ListResourcesResult(filtered, page.nextCursor(), null);
            return new JsonRpcResponse(req.id(), AbstractEntityCodec.paginatedResult(
                    "resources",
                    "resource",
                    r -> new Pagination.Page<>(r.resources(), r.nextCursor()),
                    ListResourcesResult::_meta,
                    new ResourceAbstractEntityCodec(),
                    (p, meta) -> new ListResourcesResult(p.items(), p.nextCursor(), meta)).toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        ReadResourceRequest rrr;
        try {
            rrr = (new ReadResourceRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Invalid params");
        }
        return withExistingResource(req, rrr.uri(), block -> {
            var result = new ReadResourceResult(List.of(block), null);
            return new JsonRpcResponse(req.id(), new ReadResourceResultJsonCodec().toJson(result));
        });
    }

    private JsonRpcMessage listResourceTemplates(JsonRpcRequest req) {
        try {
            var pageReq = PAGINATED_REQUEST_CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(pageReq.cursor());
            var page = resources.listTemplates(cursor);
            var filtered = page.items().stream()
                    .filter(t -> resourceAllowed(t.annotations()))
                    .toList();
            var result = new ListResourceTemplatesResult(filtered, page.nextCursor(), null);
            return new JsonRpcResponse(req.id(), AbstractEntityCodec.paginatedResult(
                    "resourceTemplates",
                    "resourceTemplate",
                    r -> new Pagination.Page<>(r.resourceTemplates(), r.nextCursor()),
                    ListResourceTemplatesResult::_meta,
                    new ResourceTemplateAbstractEntityCodec(),
                    (p, meta) -> new ListResourceTemplatesResult(p.items(), p.nextCursor(), meta)).toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req) {
        SubscribeRequest sr;
        try {
            sr = (new SubscribeRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        var uri = sr.uri();
        return withAccessibleUri(req, uri, () -> {
            if (resources.get(uri).isEmpty()) {
                return JsonRpcError.of(req.id(), -32002, "Resource not found",
                        Json.createObjectBuilder().add("uri", uri.toString()).build());
            }
            if (resourceSubscriptions.containsKey(uri)) {
                return JsonRpcError.of(req.id(), -32602, "Already subscribed to resource",
                        Json.createObjectBuilder().add("uri", uri.toString()).build());
            }
            try {
                var sub = resources.subscribe(uri, update -> {
                    try {
                        var n = new ResourceUpdatedNotification(update.uri(), update.title());
                        send(new JsonRpcNotification(NotificationMethod.RESOURCES_UPDATED.method(),
                                RESOURCE_UPDATED_NOTIFICATION_JSON_CODEC.toJson(n)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                resourceSubscriptions.put(uri, sub);
                scheduleResourceSnapshot(uri);
            } catch (Exception e) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
            }
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        });
    }

    private JsonRpcMessage unsubscribeResource(JsonRpcRequest req) {
        UnsubscribeRequest ur;
        try {
            ur = (new UnsubscribeRequestAbstractEntityCodec()).fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        var uri = ur.uri();
        return withAccessibleUri(req, uri, () -> {
            if (!resourceSubscriptions.containsKey(uri)) {
                return JsonRpcError.of(req.id(), -32602, "No active subscription for resource",
                        Json.createObjectBuilder().add("uri", uri.toString()).build());
            }
            var sub = resourceSubscriptions.remove(uri);
            CloseUtil.close(sub);
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        });
    }

    private boolean resourceAllowed(Annotations ann) {
        try {
            resourceAccess.requireAllowed(principal, ann);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    private boolean canAccessResource(URI uri) {
        if (!RootChecker.withinRoots(uri, rootsManager.roots())) {
            return false;
        }
        return resources.get(uri)
                .map(Resource::annotations)
                .map(this::resourceAllowed)
                .orElse(true);
    }

    private void sendResourceProgress(ProgressToken token, double current, String message) {
        try {
            progress.send(new ProgressNotification(token, current, null, message),
                    (method, payload) -> send(new JsonRpcNotification(method.method(), payload)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonRpcMessage withAccessibleUri(JsonRpcRequest req, URI uri, Supplier<JsonRpcMessage> action) {
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found",
                    Json.createObjectBuilder().add("uri", uri.toString()).build());
        }
        return action.get();
    }

    private JsonRpcMessage withExistingResource(JsonRpcRequest req, URI uri, Function<ResourceBlock, JsonRpcMessage> action) {
        var block = resources.read(uri);
        if (block == null) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found",
                    Json.createObjectBuilder().add("uri", uri.toString()).build());
        }
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        return action.apply(block);
    }

    private JsonRpcMessage setLogLevel(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.LOGGING);
        var params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            logLevel.set(SET_LEVEL_REQUEST_JSON_CODEC.fromJson(params).level());
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Invalid params");
        }
    }

    private void sendLog(LoggingMessageNotification note) throws IOException {
        if (rateLimit(logLimiter, note.logger() == null ? "" : note.logger()).isPresent() ||
                note.level().ordinal() < logLevel.get().ordinal()) {
            return;
        }
        requireServerCapability(ServerCapability.LOGGING);
        var params = LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC.toJson(note);
        send(new JsonRpcNotification(NotificationMethod.MESSAGE.method(), params));
    }

    private JsonRpcMessage complete(JsonRpcRequest req) {
        if (!serverCapabilities.contains(ServerCapability.COMPLETIONS)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Capability not supported");
        }
        requireServerCapability(ServerCapability.COMPLETIONS);
        var params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            var request = COMPLETE_REQUEST_JSON_CODEC.fromJson(params);
            var limit = rateLimit(completionLimiter, request.ref().toString());
            if (limit.isPresent()) {
                return JsonRpcError.of(req.id(), config.rateLimitErrorCode(), limit.get());
            }
            var result = completions.complete(request);
            return new JsonRpcResponse(req.id(), new CompleteResultJsonCodec().toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage request(RequestMethod method, JsonObject params, Duration timeout) throws IOException {
        var id = nextId();
        var future = new CompletableFuture<JsonRpcMessage>();
        pending.put(id, future);
        send(new JsonRpcRequest(id, method.method(), params));
        return awaitAndProcess(
                id,
                future,
                normalizeTimeout(timeout),
                this::receiveMessage,
                this::handleInvalidRequest,
                config.errorTimeout());
    }

    private ElicitResult elicit(ElicitRequest req) throws IOException {
        lifecycle.requireClientCapability(ClientCapability.ELICITATION);
        var msg = request(RequestMethod.ELICITATION_CREATE, new ElicitRequestJsonCodec().toJson(req), config.defaultTimeoutMs());
        if (msg instanceof JsonRpcResponse resp) {
            var er = new ElicitResultJsonCodec().fromJson(resp.result());
            if (er.action() == ElicitationAction.ACCEPT) {
                JsonSchemaValidator.validate(req.requestedSchema(), er.content());
            }
            return er;
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private CreateMessageResponse createMessage(CreateMessageRequest req) throws IOException {
        lifecycle.requireClientCapability(ClientCapability.SAMPLING);
        samplingAccess.requireAllowed(principal);
        try {
            return sampling.createMessage(req, config.defaultTimeoutMs());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private JsonRpcMessage handleCreateMessage(JsonRpcRequest req) {
        var params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            var cmr = new CreateMessageRequestJsonCodec().fromJson(params);
            var resp = createMessage(cmr);
            return new JsonRpcResponse(req.id(), new CreateMessageResponseAbstractEntityCodec().toJson(resp));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        lifecycle.shutdown();
        resourceSubscriptions.values().forEach(CloseUtil::close);
        if (resourceListSubscription != null) {
            CloseUtil.close(resourceListSubscription);
            resourceListSubscription = null;
        }
        if (resources != null) {
            resources.close();
        }
        if (toolListSubscription != null) {
            CloseUtil.close(toolListSubscription);
            toolListSubscription = null;
        }
        if (promptsSubscription != null) {
            CloseUtil.close(promptsSubscription);
            promptsSubscription = null;
        }
        if (completions != null) {
            completions.close();
        }
        if (sampling != null) {
            sampling.close();
        }
        transport.close();
    }
}
