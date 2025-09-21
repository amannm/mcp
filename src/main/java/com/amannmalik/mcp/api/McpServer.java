package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.config.McpServerConfiguration;
import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.prompts.ListPromptsResult;
import com.amannmalik.mcp.prompts.PromptListChangedNotification;
import com.amannmalik.mcp.roots.RootsManager;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.StreamableHttpServerTransport;
import com.amannmalik.mcp.util.*;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;

import java.io.EOFException;
import java.io.IOException;
import java.lang.System.Logger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/// - [Server](specification/2025-06-18/server/index.mdx)
/// - [MCP server conformance test](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:6-34)
public final class McpServer extends JsonRpcEndpoint implements AutoCloseable {
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
    private static final Logger LOG = PlatformLog.get(McpServer.class);

    private final McpServerConfiguration config;
    private final Set<ServerCapability> serverCapabilities;
    private final ResourceOrchestrator resourceOrchestrator;
    private final ToolProvider tools;
    private final PromptProvider prompts;
    private final CompletionProvider completions;
    private final SamplingProvider sampling;
    private final ToolCallHandler toolHandler;
    private final RootsManager rootsManager;
    private final SamplingAccessPolicy samplingAccess;
    private final Principal principal;
    private final RateLimiter completionLimiter;
    private final RateLimiter logLimiter;
    private final ServerLifecycle lifecycle;
    private final AtomicReference<LoggingLevel> logLevel = new AtomicReference<>();
    private AutoCloseable toolListSubscription;
    private AutoCloseable promptsSubscription;

    public McpServer(McpServerConfiguration config,
                     ResourceProvider resources,
                     ToolProvider tools,
                     PromptProvider prompts,
                     CompletionProvider completions,
                     SamplingProvider sampling,
                     ResourceAccessPolicy resourceAccess,
                     Principal principal,
                     // Instructions describing how to use the server and its features
                     // This can be used by clients to improve the LLM's understanding of available tools, resources, etc.
                     // It can be thought of like a "hint" to the model.
                     // For example, this information MAY be added to the system prompt.
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
        this.toolHandler = createToolHandler(tools, config, principal);
        this.samplingAccess = config.samplingAccessPolicy();
        this.logLevel.set(config.initialLogLevel());
        this.rootsManager = new RootsManager(lifecycle::clientCapabilities, this::request);
        this.resourceOrchestrator = resources == null ? null :
                new ResourceOrchestrator(resources, resourceAccess, principal, rootsManager, lifecycle::state, (method, params) -> send(new JsonRpcNotification(method.method(), params)), progress);
        subscribeListChanges(tools, prompts);
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
                if (config.expectedAudience() != null && !config.expectedAudience().isBlank()) {
                    var secretEnv = System.getenv("MCP_JWT_SECRET");
                    var tokenValidator = secretEnv == null || secretEnv.isBlank()
                            ? new JwtTokenValidator(config.expectedAudience())
                            : new JwtTokenValidator(config.expectedAudience(), secretEnv.getBytes(StandardCharsets.UTF_8));
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
                sendLog(LoggingLevel.ERROR, config.serverLoggerName(), Json.createValue(e.getMessage()));
            } catch (Exception e) {
                LOG.log(Logger.Level.ERROR, () -> "Unexpected " + config.errorProcessing().toLowerCase(Locale.ROOT) + ": " + e.getMessage());
                sendLog(LoggingLevel.ERROR, config.serverLoggerName(), Json.createValue(e.getMessage()));
            }
        }
    }

    private ToolCallHandler createToolHandler(ToolProvider tools,
                                              McpServerConfiguration config,
                                              Principal principal) {
        if (tools == null) {
            return null;
        }
        return new ToolCallHandler(
                tools,
                config.toolAccessPolicy(),
                limiter(config.toolsPerSecond(), config.rateLimiterWindowMs()),
                principal,
                config,
                lifecycle::clientCapabilities,
                this::elicit);
    }

    private void subscribeListChanges(ToolProvider tools, PromptProvider prompts) {
        if (tools != null && tools.supportsListChanged()) {
            toolListSubscription = SubscriptionUtil.subscribeListChanges(
                    lifecycle::state,
                    tools::onListChanged,
                    () -> send(new JsonRpcNotification(
                            NotificationMethod.TOOLS_LIST_CHANGED.method(),
                            TOOL_LIST_CHANGED_NOTIFICATION_JSON_CODEC.toJson(new ToolListChangedNotification()))));
        }
        if (prompts != null && prompts.supportsListChanged()) {
            promptsSubscription = SubscriptionUtil.subscribeListChanges(
                    lifecycle::state,
                    prompts::onListChanged,
                    () -> send(new JsonRpcNotification(
                            NotificationMethod.PROMPTS_LIST_CHANGED.method(),
                            PromptListChangedNotification.CODEC.toJson(new PromptListChangedNotification()))));
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

        if (resourceOrchestrator != null) {
            resourceOrchestrator.register(this);
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
            shutdown();
        } catch (JsonParsingException e) {
            handleParseError(e);
        } catch (IOException e) {
            LOG.log(Logger.Level.ERROR, () -> config.errorProcessing() + ": " + e.getMessage());
            try {
                sendLog(LoggingLevel.ERROR, config.serverLoggerName(), Json.createValue(e.getMessage()));
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
            sendLog(logLevel, logger, Json.createValue(message));
            send(JsonRpcError.of(id, code, message));
        } catch (IOException ioe) {
            LOG.log(Logger.Level.ERROR, () -> "Failed to send error: " + ioe.getMessage());
        }
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

    private InitializeResponse initialize(InitializeRequest request) {
        return lifecycle.initialize(request, serverFeatures());
    }

    private void initialized() {
        lifecycle.confirmInitialized();
    }

    private void shutdown() {
        lifecycle.shutdown();
    }

    private JsonRpcMessage initialize(JsonRpcRequest req) {
        var init = INITIALIZE_REQUEST_CODEC.fromJson(req.params());
        var resp = initialize(init);
        var json = INITIALIZE_RESPONSE_CODEC.toJson(resp);
        return new JsonRpcResponse(req.id(), json);
    }

    private void initialized(JsonRpcNotification ignored) {
        initialized();
        rootsManager.refreshAsync();
    }

    private JsonRpcMessage ping(JsonRpcRequest req) {
        var params = req.params();
        if (params != null) {
            if (params.size() != 1 || !params.containsKey("_meta")
                    || params.get("_meta").getValueType() != JsonValue.ValueType.OBJECT) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Invalid params");
            }
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    private void requireClientCapability(ClientCapability cap) {
        lifecycle.requireClientCapability(cap);
    }

    private Set<ServerFeature> serverFeatures() {
        var f = EnumSet.noneOf(ServerFeature.class);
        if (resourceOrchestrator != null && resourceOrchestrator.supportsSubscribe()) {
            f.add(ServerFeature.RESOURCES_SUBSCRIBE);
        }
        if (resourceOrchestrator != null && resourceOrchestrator.supportsListChanged()) {
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

    private Optional<JsonRpcError> checkInitialized(RequestId id) {
        return lifecycle.ensureInitialized(id, config.errorNotInitialized());
    }

    private Optional<JsonRpcMessage> ensureInitialized(JsonRpcRequest req) {
        return checkInitialized(req.id()).map(error -> (JsonRpcMessage) error);
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
            sendLog(LoggingLevel.INFO, config.cancellationLoggerName(), payload);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        var initCheck = ensureInitialized(req);
        if (initCheck.isPresent()) {
            return initCheck.get();
        }
        requireServerCapability(ServerCapability.TOOLS);
        try {
            var pageReq = PaginatedRequest.CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(pageReq.cursor());
            var page = tools.list(cursor);
            var json = LIST_TOOLS_RESULT_JSON_CODEC.toJson(new ListToolsResult(page.items(), page.nextCursor(), null));
            return new JsonRpcResponse(req.id(), json);
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage callTool(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.TOOLS);
        return toolHandler.handle(req);
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        var initCheck = ensureInitialized(req);
        if (initCheck.isPresent()) {
            return initCheck.get();
        }
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            var pageReq = PaginatedRequest.CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(pageReq.cursor());
            var page = prompts.list(cursor);
            return new JsonRpcResponse(req.id(), LIST_PROMPTS_RESULT_CODEC.toJson(new ListPromptsResult(page.items(), page.nextCursor(), null)));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            var getRequest = GET_PROMPT_REQUEST_JSON_CODEC.fromJson(req.params());
            var inst = prompts.get(getRequest.name(), getRequest.arguments());
            return new JsonRpcResponse(req.id(), PROMPT_INSTANCE_JSON_CODEC.toJson(inst));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
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

    private void sendLog(LoggingLevel level, String logger, JsonValue data) throws IOException {
        sendLog(new LoggingMessageNotification(level, logger, data));
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
        requireClientCapability(ClientCapability.ELICITATION);
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
        requireClientCapability(ClientCapability.SAMPLING);
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
        shutdown();
        CloseUtil.close(resourceOrchestrator);
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
