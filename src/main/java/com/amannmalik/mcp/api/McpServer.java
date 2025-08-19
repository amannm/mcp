package com.amannmalik.mcp.api;

import com.amannmalik.mcp.auth.*;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.roots.RootsManager;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.transport.StdioTransport;
import com.amannmalik.mcp.transport.StreamableHttpServerTransport;
import com.amannmalik.mcp.util.*;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/// - [Server](specification/2025-06-18/server/index.mdx)
/// - [MCP server conformance test](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:6-34)
public final class McpServer extends JsonRpcEndpoint implements AutoCloseable {
    private static final InitializeRequestAbstractEntityCodec INITIALIZE_REQUEST_CODEC = new InitializeRequestAbstractEntityCodec();
    private static final JsonCodec<LoggingMessageNotification> LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC = new LoggingMessageNotificationAbstractEntityCodec();
    private static final CompleteRequestJsonCodec COMPLETE_REQUEST_JSON_CODEC = new CompleteRequestJsonCodec();
    private static final JsonCodec<SetLevelRequest> SET_LEVEL_REQUEST_JSON_CODEC = new SetLevelRequestAbstractEntityCodec();
    private static final CancelledNotificationJsonCodec CANCELLED_NOTIFICATION_JSON_CODEC = new CancelledNotificationJsonCodec();
    private static final JsonCodec<ToolListChangedNotification> TOOL_LIST_CHANGED_NOTIFICATION_JSON_CODEC = new ToolListChangedNotificationJsonCodec();
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

    private final McpServerConfiguration config;
    private final Set<ServerCapability> serverCapabilities;
    private final ServerInfo serverInfo;
    private final String instructions;
    private final List<String> supportedVersions;
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
    private String protocolVersion;
    private LifecycleState lifecycleState = LifecycleState.INIT;
    private Set<ClientCapability> clientCapabilities = Set.of();
    private ClientFeatures clientFeatures = ClientFeatures.EMPTY;
    private AutoCloseable toolListSubscription;
    private AutoCloseable promptsSubscription;
    private volatile LoggingLevel logLevel;

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
        this.serverInfo = new ServerInfo(
                config.serverName(),
                config.serverDescription(),
                config.serverVersion());
        this.instructions = instructions;
        List<String> versions = new ArrayList<>(config.supportedVersions());
        versions.sort(Comparator.reverseOrder());
        this.supportedVersions = List.copyOf(versions);
        this.protocolVersion = this.supportedVersions.getFirst();
        this.tools = tools;
        this.prompts = prompts;
        this.completions = completions;
        this.sampling = sampling;
        this.principal = principal;
        this.toolHandler = createToolHandler(tools, config, principal);
        this.samplingAccess = config.samplingAccessPolicy();
        this.logLevel = config.initialLogLevel();
        this.rootsManager = new RootsManager(this::negotiatedClientCapabilities, this::request);
        this.resourceOrchestrator = resources == null ? null :
                new ResourceOrchestrator(resources, resourceAccess, principal, rootsManager, this::state, (method, params) -> send(new JsonRpcNotification(method.method(), params)), progress);
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
        if (resources != null) caps.add(ServerCapability.RESOURCES);
        if (tools != null) caps.add(ServerCapability.TOOLS);
        if (prompts != null) caps.add(ServerCapability.PROMPTS);
        if (completions != null) caps.add(ServerCapability.COMPLETIONS);
        caps.add(ServerCapability.LOGGING);
        return EnumSet.copyOf(caps);
    }

    private ToolCallHandler createToolHandler(ToolProvider tools,
                                              McpServerConfiguration config,
                                              Principal principal) {
        if (tools == null) return null;
        return new ToolCallHandler(
                tools,
                config.toolAccessPolicy(),
                limiter(config.toolsPerSecond(), config.rateLimiterWindowMs()),
                principal,
                config,
                () -> clientCapabilities,
                this::elicit);
    }

    private void subscribeListChanges(ToolProvider tools, PromptProvider prompts) {
        if (tools != null && tools.supportsListChanged()) {
            toolListSubscription = SubscriptionUtil.subscribeListChanges(
                    this::state,
                    tools::onListChanged,
                    () -> send(new JsonRpcNotification(
                            NotificationMethod.TOOLS_LIST_CHANGED.method(),
                            TOOL_LIST_CHANGED_NOTIFICATION_JSON_CODEC.toJson(new ToolListChangedNotification()))));
        }
        if (prompts != null && prompts.supportsListChanged()) {
            promptsSubscription = SubscriptionUtil.subscribeListChanges(
                    this::state,
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

    private static Transport createTransport(McpServerConfiguration config) throws Exception {
        return switch (config.transportType()) {
            case "stdio" -> createStdioTransport(config);
            case "http" -> createHttpTransport(config);
            default -> throw new IllegalArgumentException("Unknown transport type: " + config.transportType());
        };
    }

    private static Transport createStdioTransport(McpServerConfiguration config) {
        return new StdioTransport(System.in, System.out, config.defaultTimeoutMs());
    }

    private static Transport createHttpTransport(McpServerConfiguration config) throws Exception {
        if (!config.insecure() && config.authServers().isEmpty()) {
            throw new IllegalArgumentException("auth server must be specified");
        }
        var authManager = authorizationManager(config);
        var ht = new StreamableHttpServerTransport(config, authManager);
        if (config.verbose()) {
            if (config.serverPort() > 0) {
                System.err.println("Listening on http://127.0.0.1:" + ht.port());
            }
            if (config.httpsPort() > 0) {
                System.err.println("Listening on https://127.0.0.1:" + ht.httpsPort());
            }
        }
        return ht;
    }

    private static AuthorizationManager authorizationManager(McpServerConfiguration config) {
        if (config.expectedAudience() == null || config.expectedAudience().isBlank()) {
            return null;
        }
        var secretEnv = System.getenv("MCP_JWT_SECRET");
        var tokenValidator = (secretEnv == null || secretEnv.isBlank())
                ? new JwtTokenValidator(config.expectedAudience())
                : new JwtTokenValidator(config.expectedAudience(), secretEnv.getBytes(StandardCharsets.UTF_8));
        return new AuthorizationManager(List.of(new BearerTokenAuthorizationStrategy(tokenValidator)));
    }

    public void serve() throws IOException {
        while (state() != LifecycleState.SHUTDOWN) {
            var obj = receiveMessage();
            if (obj.isEmpty()) continue;
            try {
                process(CODEC.fromJson(obj.get()));
            } catch (IllegalArgumentException e) {
                handleInvalidRequest(e);
            } catch (IOException e) {
                System.err.println(config.errorProcessing() + ": " + e.getMessage());
                sendLog(LoggingLevel.ERROR, config.serverLoggerName(), Json.createValue(e.getMessage()));
            } catch (Exception e) {
                System.err.println("Unexpected " + config.errorProcessing().toLowerCase() + ": " + e.getMessage());
                sendLog(LoggingLevel.ERROR, config.serverLoggerName(), Json.createValue(e.getMessage()));
            }
        }
    }

    private Optional<JsonObject> receiveMessage() {
        try {
            return Optional.of(transport.receive());
        } catch (EOFException e) {
            shutdown();
        } catch (JsonParsingException e) {
            handleParseError(e);
        } catch (IOException e) {
            System.err.println(config.errorProcessing() + ": " + e.getMessage());
            try {
                sendLog(LoggingLevel.ERROR, config.serverLoggerName(), Json.createValue(e.getMessage()));
            } catch (IOException ioe) {
                System.err.println("Failed to send error: " + ioe.getMessage());
            }
        }
        return Optional.empty();
    }

    private void handleParseError(JsonParsingException e) {
        System.err.println(config.errorParse() + ": " + e.getMessage());
        try {
            sendLog(LoggingLevel.ERROR, config.parserLoggerName(), Json.createValue(e.getMessage()));
            send(JsonRpcError.of(RequestId.NullId.INSTANCE, JsonRpcErrorCode.PARSE_ERROR, e.getMessage()));
        } catch (IOException ioe) {
            System.err.println("Failed to send error: " + ioe.getMessage());
        }
    }

    private void handleInvalidRequest(IllegalArgumentException e) {
        System.err.println(config.errorInvalidRequest() + ": " + e.getMessage());
        try {
            sendLog(LoggingLevel.WARNING, config.serverLoggerName(), Json.createValue(e.getMessage()));
            send(JsonRpcError.of(RequestId.NullId.INSTANCE, JsonRpcErrorCode.INVALID_REQUEST, e.getMessage()));
        } catch (IOException ioe) {
            System.err.println("Failed to send error: " + ioe.getMessage());
        }
    }

    private InitializeResponse initialize(InitializeRequest request) {
        ensureState(LifecycleState.INIT);
        var requested = request.capabilities().client();
        clientCapabilities = requested.isEmpty()
                ? EnumSet.noneOf(ClientCapability.class)
                : EnumSet.copyOf(requested);
        clientFeatures = request.features() == null ? ClientFeatures.EMPTY : request.features();
        if (request.protocolVersion() != null && supportedVersions.contains(request.protocolVersion())) {
            protocolVersion = request.protocolVersion();
        } else {
            protocolVersion = supportedVersions.getFirst();
        }
        return new InitializeResponse(
                protocolVersion,
                new Capabilities(clientCapabilities, serverCapabilities, Map.of(), Map.of()),
                serverInfo,
                instructions,
                null
        );
    }

    private void initialized() {
        ensureState(LifecycleState.INIT);
        lifecycleState = LifecycleState.OPERATION;
    }

    private void shutdown() {
        lifecycleState = LifecycleState.SHUTDOWN;
    }

    private LifecycleState state() {
        return lifecycleState;
    }

    private Set<ClientCapability> negotiatedClientCapabilities() {
        return clientCapabilities;
    }

    private ClientFeatures clientFeatures() {
        return clientFeatures;
    }

    private Set<ServerCapability> serverCapabilities() {
        return serverCapabilities;
    }

    private String protocolVersion() {
        return protocolVersion;
    }

    private void ensureState(LifecycleState expected) {
        if (lifecycleState != expected) {
            throw new IllegalStateException("Invalid lifecycle state: " + lifecycleState);
        }
    }

    private JsonRpcMessage initialize(JsonRpcRequest req) {
        var init = INITIALIZE_REQUEST_CODEC.fromJson(req.params());
        InitializeResponse baseResp;
        try {
            baseResp = initialize(init);
        } catch (UnsupportedProtocolVersionException e) {
            return JsonRpcError.of(
                    req.id(),
                    JsonRpcErrorCode.INVALID_PARAMS,
                    "Unsupported protocol version",
                    Json.createObjectBuilder()
                            .add("supported", config.supportedVersions().stream()
                                    .collect(Json::createArrayBuilder, JsonArrayBuilder::add, (a, b) -> {
                                    })
                                    .build())
                            .add("requested", e.requested())
                            .build());
        }
        var resp = new InitializeResponse(
                baseResp.protocolVersion(),
                baseResp.capabilities(),
                baseResp.serverInfo(),
                baseResp.instructions(),
                serverFeatures()
        );
        var json = new InitializeResponseAbstractEntityCodec().toJson(resp);
        return new JsonRpcResponse(req.id(), json);
    }

    private void initialized(JsonRpcNotification ignored) {
        initialized();
        rootsManager.refreshAsync();
    }

    private JsonRpcMessage ping(JsonRpcRequest req) {
        var params = req.params();
        if (params != null) {
            if (params.isEmpty() || params.size() != 1 || !params.containsKey("_meta")
                    || params.get("_meta").getValueType() != JsonValue.ValueType.OBJECT) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Invalid params");
            }
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    private void requireClientCapability(ClientCapability cap) {
        if (!negotiatedClientCapabilities().contains(cap)) {
            throw new IllegalStateException("Missing client capability: " + cap);
        }
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
        return f.isEmpty() ? Set.of() : EnumSet.copyOf(f);
    }

    private void requireServerCapability(ServerCapability cap) {
        if (!serverCapabilities().contains(cap)) {
            throw new IllegalStateException("Server capability not declared: " + cap);
        }
    }

    private Optional<JsonRpcError> checkInitialized(RequestId id) {
        if (lifecycleState != LifecycleState.OPERATION) {
            return Optional.of(JsonRpcError.of(id, -32002, config.errorNotInitialized()));
        }
        return Optional.empty();
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
        progress.cancel(cn.requestId(), cn.reason());
        progress.release(cn.requestId());
        try {
            var reason = progress.reason(cn.requestId());
            sendLog(LoggingLevel.INFO, config.cancellationLoggerName(),
                    reason == null ? JsonValue.NULL : Json.createValue(reason));
        } catch (IOException ignore) {
        }
    }

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        var initCheck = checkInitialized(req.id());
        if (initCheck.isPresent()) return initCheck.get();
        requireServerCapability(ServerCapability.TOOLS);
        try {
            var ltr = AbstractEntityCodec.paginatedRequest(
                    ListToolsRequest::cursor,
                    ListToolsRequest::_meta,
                    ListToolsRequest::new).fromJson(req.params());
            var cursor = CursorUtil.sanitize(ltr.cursor());
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
        var initCheck = checkInitialized(req.id());
        if (initCheck.isPresent()) return initCheck.get();
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            var lpr = ListPromptsRequest.CODEC.fromJson(req.params());
            var cursor = CursorUtil.sanitize(lpr.cursor());
            var page = prompts.list(cursor);
            return new JsonRpcResponse(req.id(), LIST_PROMPTS_RESULT_CODEC.toJson(new ListPromptsResult(page.items(), page.nextCursor(), null)));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            var getRequest = ((JsonCodec<GetPromptRequest>) new GetPromptRequestAbstractEntityCodec()).fromJson(req.params());
            var inst = prompts.get(getRequest.name(), getRequest.arguments());
            return new JsonRpcResponse(req.id(), new PromptInstanceAbstractEntityCodec().toJson(inst));
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
            logLevel = SET_LEVEL_REQUEST_JSON_CODEC.fromJson(params).level();
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Invalid params");
        }
    }

    private void sendLog(LoggingMessageNotification note) throws IOException {
        if (rateLimit(logLimiter, note.logger() == null ? "" : note.logger()).isPresent() ||
                note.level().ordinal() < logLevel.ordinal()) return;
        requireServerCapability(ServerCapability.LOGGING);
        var params = LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC.toJson(note);
        send(new JsonRpcNotification(NotificationMethod.MESSAGE.method(), params));
    }

    private void sendLog(LoggingLevel level, String logger, JsonValue data) throws IOException {
        sendLog(new LoggingMessageNotification(level, logger, data));
    }

    private JsonRpcMessage complete(JsonRpcRequest req) {
        if (!serverCapabilities().contains(ServerCapability.COMPLETIONS)) {
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

    private JsonRpcMessage request(RequestMethod method, JsonObject params, long timeoutMillis) throws IOException {
        var id = nextId();
        var future = new CompletableFuture<JsonRpcMessage>();
        pending.put(id, future);
        send(new JsonRpcRequest(id, method.method(), params));
        return awaitAndProcess(
                id,
                future,
                Duration.ofMillis(timeoutMillis),
                this::receiveMessage,
                this::handleInvalidRequest,
                config.errorTimeout());
    }

    private ElicitResult elicit(ElicitRequest req) throws IOException {
        requireClientCapability(ClientCapability.ELICITATION);
        var msg = request(RequestMethod.ELICITATION_CREATE, new ElicitRequestJsonCodec().toJson(req), 0L);
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
        CloseUtil.closeQuietly(resourceOrchestrator);
        if (toolListSubscription != null) {
            CloseUtil.closeQuietly(toolListSubscription);
            toolListSubscription = null;
        }
        if (promptsSubscription != null) {
            CloseUtil.closeQuietly(promptsSubscription);
            promptsSubscription = null;
        }
        if (completions != null) completions.close();
        if (sampling != null) sampling.close();
        transport.close();
    }
}
