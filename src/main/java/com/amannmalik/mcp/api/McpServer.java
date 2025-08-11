package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.model.*;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.core.*;
import com.amannmalik.mcp.spi.ElicitationAction;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.roots.RootsManager;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.tools.ToolListChangedNotification;
import com.amannmalik.mcp.transport.Protocol;
import com.amannmalik.mcp.util.*;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/// - [Server](specification/2025-06-18/server/index.mdx)
/// - [MCP server conformance test](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:6-34)
public final class McpServer extends JsonRpcEndpoint implements AutoCloseable {
    private static final int RATE_LIMIT_CODE = McpConfiguration.current().rateLimit();
    private static final InitializeRequestAbstractEntityCodec INITIALIZE_REQUEST_CODEC = new InitializeRequestAbstractEntityCodec();
    private static final JsonCodec<LoggingMessageNotification> LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC = new LoggingMessageNotificationAbstractEntityCodec();
    private static final CallToolRequestAbstractEntityCodec CALL_TOOL_REQUEST_CODEC = new CallToolRequestAbstractEntityCodec();
    private static final JsonCodec<ToolResult> TOOL_RESULT_ABSTRACT_ENTITY_CODEC = new ToolResultAbstractEntityCodec();
    private static final CompleteRequestJsonCodec COMPLETE_REQUEST_JSON_CODEC = new CompleteRequestJsonCodec();
    private static final JsonCodec<SetLevelRequest> SET_LEVEL_REQUEST_JSON_CODEC = new SetLevelRequestAbstractEntityCodec();
    private static final CancelledNotificationJsonCodec CANCELLED_NOTIFICATION_JSON_CODEC = new CancelledNotificationJsonCodec();

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
    private final Set<ServerCapability> serverCapabilities;
    private final ServerInfo serverInfo;
    private final String instructions;
    private final List<String> supportedVersions;
    private final ResourceOrchestrator resourceOrchestrator;
    private final ToolProvider tools;
    private final PromptProvider prompts;
    private final CompletionProvider completions;
    private final SamplingProvider sampling;
    private final RootsManager rootsManager;
    private final ToolAccessPolicy toolAccess;
    private final SamplingAccessPolicy samplingAccess;
    private final Principal principal;
    private final RateLimiter toolLimiter = new RateLimiter(
            McpConfiguration.current().toolsPerSecond(),
            McpConfiguration.current().rateLimiterWindowMs());
    private final RateLimiter completionLimiter = new RateLimiter(
            McpConfiguration.current().completionsPerSecond(),
            McpConfiguration.current().rateLimiterWindowMs());
    private final RateLimiter logLimiter = new RateLimiter(
            McpConfiguration.current().logsPerSecond(),
            McpConfiguration.current().rateLimiterWindowMs());
    private String protocolVersion;
    private LifecycleState lifecycleState = LifecycleState.INIT;
    private Set<ClientCapability> clientCapabilities = Set.of();
    private ClientFeatures clientFeatures = ClientFeatures.EMPTY;
    private ChangeSubscription toolListSubscription;
    private ChangeSubscription promptsSubscription;
    private volatile LoggingLevel logLevel = LoggingLevel.INFO;


    public McpServer(ResourceProvider resources,
                     ToolProvider tools,
                     PromptProvider prompts,
                     CompletionProvider completions,
                     SamplingProvider sampling,
                     ResourceAccessPolicy resourceAccess,
                     ToolAccessPolicy toolAccess,
                     SamplingAccessPolicy samplingAccess,
                     Principal principal,
                     // Instructions describing how to use the server and its features
                     // This can be used by clients to improve the LLM's understanding of available tools, resources, etc.
                     // It can be thought of like a "hint" to the model.
                     // For example, this information MAY be added to the system prompt.
                     String instructions,
                     Transport transport) {
        super(transport,
                new ProgressManager(new RateLimiter(McpConfiguration.current().progressPerSecond(),
                        McpConfiguration.current().rateLimiterWindowMs())),
                McpConfiguration.current().initialRequestId());
        EnumSet<ServerCapability> caps = EnumSet.noneOf(ServerCapability.class);
        if (resources != null) caps.add(ServerCapability.RESOURCES);
        if (tools != null) caps.add(ServerCapability.TOOLS);
        if (prompts != null) caps.add(ServerCapability.PROMPTS);
        if (completions != null) caps.add(ServerCapability.COMPLETIONS);
        caps.add(ServerCapability.LOGGING);
        this.serverCapabilities = EnumSet.copyOf(caps);
        this.serverInfo = new ServerInfo(
                McpConfiguration.current().serverName(),
                McpConfiguration.current().serverDescription(),
                McpConfiguration.current().serverVersion());
        this.instructions = instructions;
        List<String> versions = new ArrayList<>(Set.of(Protocol.LATEST_VERSION, Protocol.PREVIOUS_VERSION));
        versions.sort(Comparator.reverseOrder());
        this.supportedVersions = List.copyOf(versions);
        this.protocolVersion = this.supportedVersions.getFirst();
        this.tools = tools;
        this.prompts = prompts;
        this.completions = completions;
        this.sampling = sampling;
        this.toolAccess = toolAccess == null ? ToolAccessPolicy.PERMISSIVE : toolAccess;
        this.samplingAccess = samplingAccess == null ? SamplingAccessPolicy.PERMISSIVE : samplingAccess;
        this.principal = principal;
        this.rootsManager = new RootsManager(this::negotiatedClientCapabilities, this::request);
        this.resourceOrchestrator = resources == null ? null :
                new ResourceOrchestrator(resources, resourceAccess, principal, rootsManager, this::state, this::send, progress);

        if (tools != null && tools.supportsListChanged()) {
            toolListSubscription = subscribeListChanges(
                    tools::subscribe,
                    NotificationMethod.TOOLS_LIST_CHANGED,
                    AbstractEntityCodec.empty(ToolListChangedNotification::new).toJson(new ToolListChangedNotification()));
        }

        if (prompts != null && prompts.supportsListChanged()) {
            promptsSubscription = subscribeListChanges(
                    prompts::subscribe,
                    NotificationMethod.PROMPTS_LIST_CHANGED,
                    PromptListChangedNotification.CODEC.toJson(new PromptListChangedNotification()));
        }

        registerRequest(RequestMethod.INITIALIZE.method(), this::initialize);
        registerNotification(NotificationMethod.INITIALIZED.method(), this::initialized);
        registerRequest(RequestMethod.PING.method(), this::ping);
        registerNotification(NotificationMethod.CANCELLED.method(), this::cancelled);
        registerNotification(NotificationMethod.ROOTS_LIST_CHANGED.method(), n -> rootsManager.listChangedNotification());

        if (resourceOrchestrator != null) {
            resourceOrchestrator.register(this);
        }

        if (tools != null) {
            registerRequest(RequestMethod.TOOLS_LIST.method(), this::listTools);
            registerRequest(RequestMethod.TOOLS_CALL.method(), this::callTool);
        }

        if (prompts != null) {
            registerRequest(RequestMethod.PROMPTS_LIST.method(), this::listPrompts);
            registerRequest(RequestMethod.PROMPTS_GET.method(), this::getPrompt);
        }

        registerRequest(RequestMethod.LOGGING_SET_LEVEL.method(), this::setLogLevel);

        if (completions != null) {
            registerRequest(RequestMethod.COMPLETION_COMPLETE.method(), this::complete);
        }

        registerRequest(RequestMethod.SAMPLING_CREATE_MESSAGE.method(), this::handleCreateMessage);
    }

    private <S extends ChangeSubscription> S subscribeListChanges(
            SubscriptionFactory<S> factory,
            NotificationMethod method,
            JsonObject payload) {
        try {
            return factory.subscribe(ignored -> {
                if (state() != LifecycleState.OPERATION) return;
                try {
                    send(new JsonRpcNotification(method.method(), payload));
                } catch (IOException ignore) {
                }
            });
        } catch (RuntimeException ignore) {
            return null;
        }
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
                System.err.println(McpConfiguration.current().errorProcessing() + ": " + e.getMessage());
                sendLog(LoggingLevel.ERROR, McpConfiguration.current().loggerServer(), Json.createValue(e.getMessage()));
            } catch (Exception e) {
                System.err.println("Unexpected " + McpConfiguration.current().errorProcessing().toLowerCase() + ": " + e.getMessage());
                sendLog(LoggingLevel.ERROR, McpConfiguration.current().loggerServer(), Json.createValue(e.getMessage()));
            }
        }
    }

    private Optional<JsonObject> receiveMessage() throws IOException {
        try {
            return Optional.of(transport.receive());
        } catch (EOFException e) {
            shutdown();
            return Optional.empty();
        } catch (JsonParsingException e) {
            handleParseError(e);
            return Optional.empty();
        }
    }

    private void handleParseError(JsonParsingException e) {
        System.err.println(McpConfiguration.current().errorParse() + ": " + e.getMessage());
        try {
            sendLog(LoggingLevel.ERROR, McpConfiguration.current().loggerParser(), Json.createValue(e.getMessage()));
            send(JsonRpcError.of(RequestId.NullId.INSTANCE, JsonRpcErrorCode.PARSE_ERROR, e.getMessage()));
        } catch (IOException ioe) {
            System.err.println("Failed to send error: " + ioe.getMessage());
        }
    }

    private void handleInvalidRequest(IllegalArgumentException e) {
        System.err.println(McpConfiguration.current().errorInvalidRequest() + ": " + e.getMessage());
        try {
            sendLog(LoggingLevel.WARNING, McpConfiguration.current().loggerServer(), Json.createValue(e.getMessage()));
            send(JsonRpcError.of(RequestId.NullId.INSTANCE, JsonRpcErrorCode.INVALID_REQUEST, e.getMessage()));
        } catch (IOException ioe) {
            System.err.println("Failed to send error: " + ioe.getMessage());
        }
    }

    private InitializeResponse initialize(InitializeRequest request) {
        ensureState(LifecycleState.INIT);
        Set<ClientCapability> requested = request.capabilities().client();
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
        InitializeRequest init = INITIALIZE_REQUEST_CODEC.fromJson(req.params());
        InitializeResponse baseResp;
        try {
            baseResp = initialize(init);
        } catch (UnsupportedProtocolVersionException e) {
            return JsonRpcError.of(
                    req.id(),
                    JsonRpcErrorCode.INVALID_PARAMS,
                    "Unsupported protocol version",
                    Json.createObjectBuilder()
                            .add("supported", Json.createArrayBuilder()
                                    .add(Protocol.LATEST_VERSION)
                                    .add(Protocol.PREVIOUS_VERSION)
                                    .build())
                            .add("requested", e.requested())
                            .build());
        }
        InitializeResponse resp = new InitializeResponse(
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
        JsonObject params = req.params();
        if (params != null && !params.isEmpty()) {
            ValidationUtil.requireMeta(params);
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    private void requireClientCapability(ClientCapability cap) {
        if (!negotiatedClientCapabilities().contains(cap)) {
            throw new IllegalStateException("Missing client capability: " + cap);
        }
    }

    private ServerFeatures serverFeatures() {
        return new ServerFeatures(
                resourceOrchestrator != null && resourceOrchestrator.supportsSubscribe(),
                resourceOrchestrator != null && resourceOrchestrator.supportsListChanged(),
                tools != null && tools.supportsListChanged(),
                prompts != null && prompts.supportsListChanged()
        );
    }

    private void requireServerCapability(ServerCapability cap) {
        if (!serverCapabilities().contains(cap)) {
            throw new IllegalStateException("Server capability not declared: " + cap);
        }
    }

    private Optional<JsonRpcError> checkInitialized(RequestId id) {
        if (lifecycleState != LifecycleState.OPERATION) {
            return Optional.of(JsonRpcError.of(id, -32002, McpConfiguration.current().errorNotInitialized()));
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
        CancelledNotification cn = CANCELLED_NOTIFICATION_JSON_CODEC.fromJson(note.params());
        progress.cancel(cn.requestId(), cn.reason());
        progress.release(cn.requestId());
        try {
            String reason = progress.reason(cn.requestId());
            sendLog(LoggingLevel.INFO, McpConfiguration.current().loggerCancellation(),
                    reason == null ? JsonValue.NULL : Json.createValue(reason));
        } catch (IOException ignore) {
        }
    }

    private String sanitizeCursor(String cursor) {
        return cursor == null ? null : Pagination.sanitize(ValidationUtil.cleanNullable(cursor));
    }

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        Optional<JsonRpcError> initCheck = checkInitialized(req.id());
        if (initCheck.isPresent()) return initCheck.get();
        requireServerCapability(ServerCapability.TOOLS);
        try {
            ListToolsRequest ltr = AbstractEntityCodec.paginatedRequest(
                    ListToolsRequest::cursor,
                    ListToolsRequest::_meta,
                    ListToolsRequest::new).fromJson(req.params());
            String cursor = sanitizeCursor(ltr.cursor());
            Pagination.Page<Tool> page = tools.list(cursor);
            JsonObject json = LIST_TOOLS_RESULT_JSON_CODEC.toJson(new ListToolsResult(page.items(), page.nextCursor(), null));
            return new JsonRpcResponse(req.id(), json);
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
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
        Optional<String> limit = rateLimit(toolLimiter, callRequest.name());
        if (limit.isPresent()) {
            return JsonRpcError.of(req.id(), RATE_LIMIT_CODE, limit.get());
        }
        try {
            toolAccess.requireAllowed(principal, callRequest.name());
        } catch (SecurityException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, McpConfiguration.current().errorAccessDenied());
        }
        try {
            ToolResult result = tools.call(callRequest.name(), callRequest.arguments());
            return new JsonRpcResponse(req.id(), TOOL_RESULT_ABSTRACT_ENTITY_CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return handleToolCallFailure(req, callRequest, e);
        }
    }

    private JsonRpcMessage handleToolCallFailure(JsonRpcRequest req, CallToolRequest callRequest, IllegalArgumentException e) {
        Optional<Tool> tool = tools.find(callRequest.name());
        if (tool.isPresent() && negotiatedClientCapabilities().contains(ClientCapability.ELICITATION)) {
            try {
                ElicitRequest er = new ElicitRequest(
                        "Provide arguments for tool '" + tool.get().name() + "'",
                        tool.get().inputSchema(),
                        null);
                ElicitResult res = elicit(er);
                if (res.action() == ElicitationAction.ACCEPT) {
                    try {
                        ToolResult result = tools.call(callRequest.name(), res.content());
                        return new JsonRpcResponse(req.id(), TOOL_RESULT_ABSTRACT_ENTITY_CODEC.toJson(result));
                    } catch (IllegalArgumentException ex) {
                        return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, ex.getMessage());
                    }
                }
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Tool invocation cancelled");
            } catch (IllegalArgumentException ex) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, ex.getMessage());
            } catch (Exception ex) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, ex.getMessage());
            }
        }
        return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        Optional<JsonRpcError> initCheck = checkInitialized(req.id());
        if (initCheck.isPresent()) return initCheck.get();
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            ListPromptsRequest lpr = ListPromptsRequest.CODEC.fromJson(req.params());
            String cursor = sanitizeCursor(lpr.cursor());
            Pagination.Page<Prompt> page = prompts.list(cursor);
            return new JsonRpcResponse(req.id(), LIST_PROMPTS_RESULT_CODEC.toJson(new ListPromptsResult(page.items(), page.nextCursor(), null)));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            GetPromptRequest getRequest = ((JsonCodec<GetPromptRequest>) new GetPromptRequestAbstractEntityCodec()).fromJson(req.params());
            PromptInstance inst = prompts.get(getRequest.name(), getRequest.arguments());
            return new JsonRpcResponse(req.id(), new PromptInstanceAbstractEntityCodec().toJson(inst));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage setLogLevel(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.LOGGING);
        JsonObject params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            logLevel = SET_LEVEL_REQUEST_JSON_CODEC.fromJson(params).level();
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private void sendLog(LoggingMessageNotification note) throws IOException {
        if (rateLimit(logLimiter, note.logger() == null ? "" : note.logger()).isPresent() ||
                note.level().ordinal() < logLevel.ordinal()) return;
        requireServerCapability(ServerCapability.LOGGING);
        send(new JsonRpcNotification(NotificationMethod.MESSAGE.method(),
                LOGGING_MESSAGE_NOTIFICATION_JSON_CODEC.toJson(note)));
    }

    private void sendLog(LoggingLevel level, String logger, JsonValue data) throws IOException {
        sendLog(new LoggingMessageNotification(level, logger, data));
    }

    private JsonRpcMessage complete(JsonRpcRequest req) {
        if (!serverCapabilities().contains(ServerCapability.COMPLETIONS)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Capability not supported");
        }
        requireServerCapability(ServerCapability.COMPLETIONS);
        JsonObject params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            CompleteRequest request = COMPLETE_REQUEST_JSON_CODEC.fromJson(params);
            Optional<String> limit = rateLimit(completionLimiter, request.ref().toString());
            if (limit.isPresent()) {
                return JsonRpcError.of(req.id(), RATE_LIMIT_CODE, limit.get());
            }
            CompleteResult result = completions.complete(request);
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
        RequestId id = nextId();
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(id, future);
        send(new JsonRpcRequest(id, method.method(), params));
        long end = System.currentTimeMillis() + timeoutMillis;
        while (true) {
            if (future.isDone()) {
                try {
                    return future.get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    pending.remove(id);
                    throw new IOException(e);
                } catch (ExecutionException e) {
                    pending.remove(id);
                    var cause = e.getCause();
                    if (cause instanceof IOException io) throw io;
                    throw new IOException(cause);
                }
            }
            if (System.currentTimeMillis() >= end) {
                try {
                    send(new JsonRpcNotification(
                            NotificationMethod.CANCELLED.method(),
                            CANCELLED_NOTIFICATION_JSON_CODEC.toJson(new CancelledNotification(id, "timeout"))));
                } catch (IOException ignore) {
                }
                pending.remove(id);
                throw new IOException(McpConfiguration.current().errorTimeout() + " after " + timeoutMillis + " ms");
            }
            var obj = receiveMessage();
            if (obj.isEmpty()) continue;
            try {
                process(CODEC.fromJson(obj.get()));
            } catch (IllegalArgumentException ex) {
                handleInvalidRequest(ex);
            }
        }
    }

    private ElicitResult elicit(ElicitRequest req) throws IOException {
        requireClientCapability(ClientCapability.ELICITATION);
        JsonRpcMessage msg = request(RequestMethod.ELICITATION_CREATE, new ElicitRequestJsonCodec().toJson(req), 0L);
        if (msg instanceof JsonRpcResponse resp) {
            ElicitResult er = new ElicitResultJsonCodec().fromJson(resp.result());
            if (er.action() == ElicitationAction.ACCEPT) {
                ValidationUtil.validateSchema(req.requestedSchema(), er.content());
            }
            return er;
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private CreateMessageResponse createMessage(CreateMessageRequest req) throws IOException {
        requireClientCapability(ClientCapability.SAMPLING);
        samplingAccess.requireAllowed(principal);
        try {
            return sampling.createMessage(req, McpConfiguration.current().defaultMs());
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private JsonRpcMessage handleCreateMessage(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            CreateMessageRequest cmr = new CreateMessageRequestJsonCodec().fromJson(params);
            CreateMessageResponse resp = createMessage(cmr);
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

    @FunctionalInterface
    private interface SubscriptionFactory<S extends ChangeSubscription> {
        S subscribe(Consumer<Change> listener);
    }
}
