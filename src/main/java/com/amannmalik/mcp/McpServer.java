package com.amannmalik.mcp;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.completion.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.elicitation.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.logging.*;
import com.amannmalik.mcp.ping.*;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.resources.*;
import com.amannmalik.mcp.roots.*;
import com.amannmalik.mcp.sampling.*;
import com.amannmalik.mcp.tools.*;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.ValidationUtil;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


/// - [Server](specification/2025-06-18/server/index.mdx)
/// - [MCP server conformance test](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:6-34)
public final class McpServer implements AutoCloseable {
    private final Transport transport;
    private final ProtocolLifecycle lifecycle;
    private final JsonRpcRequestProcessor processor;
    private final ProgressManager progress = new ProgressManager(
            new RateLimiter(McpConfiguration.current().progressPerSecond(),
                    McpConfiguration.current().rateLimiterWindowMs()));
    private final ResourceFeature resourceFeature;
    private final ToolProvider tools;
    private final PromptProvider prompts;
    private final CompletionProvider completions;
    private final SamplingProvider sampling;
    private ChangeSubscription toolListSubscription;
    private ChangeSubscription promptsSubscription;
    private final RootsManager rootsManager;
    private final ResourceAccessController resourceAccess;
    private final ToolAccessPolicy toolAccess;
    private final SamplingAccessPolicy samplingAccess;
    private final Principal principal;
    private volatile LoggingLevel logLevel = LoggingLevel.INFO;
    private static final int RATE_LIMIT_CODE = McpConfiguration.current().rateLimit();
    private final RateLimiter toolLimiter = new RateLimiter(
            McpConfiguration.current().toolsPerSecond(),
            McpConfiguration.current().rateLimiterWindowMs());
    private final RateLimiter completionLimiter = new RateLimiter(
            McpConfiguration.current().completionsPerSecond(),
            McpConfiguration.current().rateLimiterWindowMs());
    private final RateLimiter logLimiter = new RateLimiter(
            McpConfiguration.current().logsPerSecond(),
            McpConfiguration.current().rateLimiterWindowMs());
    private final AtomicLong requestCounter = new AtomicLong(McpConfiguration.current().initialRequestId());
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();

    public McpServer(Transport transport, String instructions) {
        this(Locator.resources(),
                Locator.tools(),
                Locator.prompts(),
                Locator.completions(),
                Locator.sampling(),
                Locator.privacyBoundary(McpConfiguration.current().defaultBoundary()),
                Locator.toolAccess(),
                Locator.samplingAccess(),
                new Principal(McpConfiguration.current().defaultPrincipal(), Set.of()),
                instructions,
                transport);
    }

    McpServer(ResourceProvider resources,
              ToolProvider tools,
              PromptProvider prompts,
              CompletionProvider completions,
              SamplingProvider sampling,
              ResourceAccessController resourceAccess,
              ToolAccessPolicy toolAccess,
              SamplingAccessPolicy samplingAccess,
              Principal principal,
              String instructions,
              Transport transport) {
        this.transport = transport;
        EnumSet<ServerCapability> caps = EnumSet.noneOf(ServerCapability.class);
        if (resources != null) caps.add(ServerCapability.RESOURCES);
        if (tools != null) caps.add(ServerCapability.TOOLS);
        if (prompts != null) caps.add(ServerCapability.PROMPTS);
        if (completions != null) caps.add(ServerCapability.COMPLETIONS);
        caps.add(ServerCapability.LOGGING);
        this.lifecycle = new ProtocolLifecycle(caps, new ServerInfo(
                McpConfiguration.current().serverName(),
                McpConfiguration.current().serverDescription(),
                McpConfiguration.current().serverVersion()), instructions);
        this.tools = tools;
        this.prompts = prompts;
        this.completions = completions;
        this.sampling = sampling;
        this.resourceAccess = resourceAccess;
        this.toolAccess = toolAccess == null ? ToolAccessPolicy.PERMISSIVE : toolAccess;
        this.samplingAccess = samplingAccess == null ? SamplingAccessPolicy.PERMISSIVE : samplingAccess;
        var requestProcessor = new JsonRpcRequestProcessor(progress, this::send);
        this.processor = requestProcessor;
        this.principal = principal;
        this.rootsManager = new RootsManager(lifecycle, this::sendRequest);
        this.resourceFeature = resources == null ? null :
                new ResourceFeature(resources, resourceAccess, principal, rootsManager, lifecycle, this::send, progress);

        if (tools != null && tools.supportsListChanged()) {
            toolListSubscription = subscribeListChanges(
                    tools::subscribeList,
                    NotificationMethod.TOOLS_LIST_CHANGED,
                    ToolListChangedNotification.CODEC.toJson(new ToolListChangedNotification()));
        }

        if (prompts != null && prompts.supportsListChanged()) {
            promptsSubscription = subscribeListChanges(
                    prompts::subscribeList,
                    NotificationMethod.PROMPTS_LIST_CHANGED,
                    PromptListChangedNotification.CODEC.toJson(new PromptListChangedNotification()));
        }

        processor.registerRequest(RequestMethod.INITIALIZE.method(), this::initialize);
        processor.registerNotification(NotificationMethod.INITIALIZED.method(), this::initialized);
        processor.registerRequest(RequestMethod.PING.method(), this::ping);
        processor.registerNotification(NotificationMethod.CANCELLED.method(), this::cancelled);
        processor.registerNotification(NotificationMethod.ROOTS_LIST_CHANGED.method(), n -> rootsManager.listChangedNotification());

        if (resourceFeature != null) {
            resourceFeature.register(processor);
        }

        if (tools != null) {
            processor.registerRequest(RequestMethod.TOOLS_LIST.method(), this::listTools);
            processor.registerRequest(RequestMethod.TOOLS_CALL.method(), this::callTool);
        }

        if (prompts != null) {
            processor.registerRequest(RequestMethod.PROMPTS_LIST.method(), this::listPrompts);
            processor.registerRequest(RequestMethod.PROMPTS_GET.method(), this::getPrompt);
        }

        processor.registerRequest(RequestMethod.LOGGING_SET_LEVEL.method(), this::setLogLevel);

        if (completions != null) {
            processor.registerRequest(RequestMethod.COMPLETION_COMPLETE.method(), this::complete);
        }

        processor.registerRequest(RequestMethod.SAMPLING_CREATE_MESSAGE.method(), this::handleCreateMessage);
    }

    private <S extends ChangeSubscription> S subscribeListChanges(
            SubscriptionFactory<S> factory,
            NotificationMethod method,
            JsonObject payload) {
        try {
            return factory.subscribe(v -> {
                if (lifecycle.state() != LifecycleState.OPERATION) return;
                try {
                    send(new JsonRpcNotification(method.method(), payload));
                } catch (IOException ignore) {
                }
            });
        } catch (RuntimeException ignore) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SubscriptionFactory<S extends ChangeSubscription> {
        S subscribe(ChangeListener<Void> listener);
    }

    public void serve() throws IOException {
        while (lifecycle.state() != LifecycleState.SHUTDOWN) {
            var obj = receiveMessage();
            if (obj.isEmpty()) continue;
            try {
                processMessage(JsonRpcCodec.CODEC.fromJson(obj.get()));
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
            lifecycle.shutdown();
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

    private void processMessage(JsonRpcMessage msg) throws IOException {
        switch (msg) {
            case JsonRpcRequest req -> onRequest(req);
            case JsonRpcNotification note -> onNotification(note);
            case JsonRpcResponse resp -> {
                var f = pending.remove(resp.id());
                if (f != null) f.complete(resp);
            }
            case JsonRpcError err -> {
                var f = pending.remove(err.id());
                if (f != null) f.complete(err);
            }
            default -> {
            }
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

    private void onRequest(JsonRpcRequest req) throws IOException {
        if (lifecycle.state() == LifecycleState.INIT &&
                RequestMethod.from(req.method())
                        .filter(m -> m != RequestMethod.INITIALIZE && m != RequestMethod.PING)
                        .isPresent()) {
            send(JsonRpcError.of(req.id(),
                    JsonRpcErrorCode.INTERNAL_ERROR,
                    McpConfiguration.current().errorNotInitialized(),
                    null));
            return;
        }

        boolean cancellable = RequestMethod.from(req.method()).map(m -> m != RequestMethod.INITIALIZE).orElse(true);
        var resp = processor.handle(req, cancellable);
        if (resp.isPresent()) send(resp.get());
    }

    private void onNotification(JsonRpcNotification note) {
        processor.handle(note);
    }

    private JsonRpcMessage initialize(JsonRpcRequest req) {
        InitializeRequest init = InitializeRequest.CODEC.fromJson(req.params());
        InitializeResponse baseResp;
        try {
            baseResp = lifecycle.initialize(init);
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
        var json = InitializeResponse.CODEC.toJson(resp);
        return new JsonRpcResponse(req.id(), json);
    }

    private void initialized(JsonRpcNotification ignored) {
        lifecycle.initialized();
        rootsManager.refreshAsync();
    }

    private JsonRpcMessage ping(JsonRpcRequest req) {
        PingRequest.CODEC.fromJson(req.params());
        return new JsonRpcResponse(req.id(), PingResponse.CODEC.toJson(new PingResponse()));
    }

    private synchronized void send(JsonRpcMessage msg) throws IOException {
        transport.send(JsonRpcCodec.CODEC.toJson(msg));
    }

    private void requireClientCapability(ClientCapability cap) {
        if (!lifecycle.negotiatedClientCapabilities().contains(cap)) {
            throw new IllegalStateException("Missing client capability: " + cap);
        }
    }

    private ServerFeatures serverFeatures() {
        return new ServerFeatures(
                resourceFeature != null && resourceFeature.supportsSubscribe(),
                resourceFeature != null && resourceFeature.supportsListChanged(),
                tools != null && tools.supportsListChanged(),
                prompts != null && prompts.supportsListChanged()
        );
    }

    private void requireServerCapability(ServerCapability cap) {
        if (!lifecycle.serverCapabilities().contains(cap)) {
            throw new IllegalStateException("Server capability not declared: " + cap);
        }
    }

    private boolean allowed(Annotations ann) {
        try {
            resourceAccess.requireAllowed(principal, ann);
            return true;
        } catch (SecurityException e) {
            return false;
        }
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
        CancelledNotification cn = CancelledNotification.CODEC.fromJson(note.params());
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
        requireServerCapability(ServerCapability.TOOLS);
        try {
            ListToolsRequest ltr = ListToolsRequest.CODEC.fromJson(req.params());
            String cursor = sanitizeCursor(ltr.cursor());
            Pagination.Page<Tool> page = tools.list(cursor);
            JsonObject json = ListToolsResult.CODEC.toJson(new ListToolsResult(page.items(), page.nextCursor(), null));
            return new JsonRpcResponse(req.id(), json);
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage callTool(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.TOOLS);
        CallToolRequest callRequest;
        try {
            callRequest = CallToolRequest.CODEC.fromJson(req.params());
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
            return new JsonRpcResponse(req.id(), ToolResult.CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return handleToolCallFailure(req, callRequest, e);
        }
    }

    private JsonRpcMessage handleToolCallFailure(JsonRpcRequest req, CallToolRequest callRequest, IllegalArgumentException e) {
        Optional<Tool> tool = tools.find(callRequest.name());
        if (tool.isPresent() && lifecycle.negotiatedClientCapabilities().contains(ClientCapability.ELICITATION)) {
            try {
                ElicitRequest er = new ElicitRequest(
                        "Provide arguments for tool '" + tool.get().name() + "'",
                        tool.get().inputSchema(),
                        null);
                ElicitResult res = elicit(er);
                if (res.action() == ElicitationAction.ACCEPT) {
                    try {
                        ToolResult result = tools.call(callRequest.name(), res.content());
                        return new JsonRpcResponse(req.id(), ToolResult.CODEC.toJson(result));
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
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            ListPromptsRequest lpr = ListPromptsRequest.CODEC.fromJson(req.params());
            String cursor = sanitizeCursor(lpr.cursor());
            Pagination.Page<Prompt> page = prompts.list(cursor);
            return new JsonRpcResponse(req.id(), ListPromptsResult.CODEC.toJson(new ListPromptsResult(page.items(), page.nextCursor(), null)));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            GetPromptRequest getRequest = GetPromptRequest.CODEC.fromJson(req.params());
            PromptInstance inst = prompts.get(getRequest.name(), getRequest.arguments());
            return new JsonRpcResponse(req.id(), PromptInstance.CODEC.toJson(inst));
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
            logLevel = SetLevelRequest.CODEC.fromJson(params).level();
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
                LoggingMessageNotification.CODEC.toJson(note)));
    }

    private void sendLog(LoggingLevel level, String logger, JsonValue data) throws IOException {
        sendLog(new LoggingMessageNotification(level, logger, data));
    }

    private JsonRpcMessage complete(JsonRpcRequest req) {
        if (!lifecycle.serverCapabilities().contains(ServerCapability.COMPLETIONS)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.METHOD_NOT_FOUND, "Capability not supported");
        }
        requireServerCapability(ServerCapability.COMPLETIONS);
        JsonObject params = req.params();
        if (params == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Missing params");
        }
        try {
            CompleteRequest request = CompleteRequest.CODEC.fromJson(params);
            Optional<String> limit = rateLimit(completionLimiter, request.ref().toString());
            if (limit.isPresent()) {
                return JsonRpcError.of(req.id(), RATE_LIMIT_CODE, limit.get());
            }
            CompleteResult result = completions.complete(request);
            return new JsonRpcResponse(req.id(), CompleteResult.CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private JsonRpcMessage sendRequest(RequestMethod method, JsonObject params) throws IOException {
        return sendRequest(method.method(), params, Timeouts.DEFAULT_TIMEOUT_MS);
    }

    private JsonRpcMessage sendRequest(String method, JsonObject params, long timeoutMillis) throws IOException {
        RequestId id = new RequestId.NumericId(requestCounter.getAndIncrement());
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(id, future);
        send(new JsonRpcRequest(id, method, params));
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
                    Throwable cause = e.getCause();
                    if (cause instanceof IOException io) throw io;
                    throw new IOException(cause);
                }
            }
            if (System.currentTimeMillis() >= end) {
                try {
                    send(new JsonRpcNotification(
                            NotificationMethod.CANCELLED.method(),
                            CancelledNotification.CODEC.toJson(new CancelledNotification(id, "timeout"))));
                } catch (IOException ignore) {
                }
                pending.remove(id);
                throw new IOException(McpConfiguration.current().errorTimeout() + " after " + timeoutMillis + " ms");
            }
            var obj = receiveMessage();
            if (obj.isEmpty()) continue;
            try {
                processMessage(JsonRpcCodec.CODEC.fromJson(obj.get()));
            } catch (IllegalArgumentException ex) {
                handleInvalidRequest(ex);
            }
        }
    }

    public List<Root> listRoots() throws IOException {
        return rootsManager.listRoots();
    }

    public ChangeSubscription subscribeRoots(ChangeListener<Void> listener) {
        return rootsManager.subscribe(listener);
    }

    public List<Root> roots() {
        return rootsManager.roots();
    }

    public ElicitResult elicit(ElicitRequest req) throws IOException {
        requireClientCapability(ClientCapability.ELICITATION);
        JsonRpcMessage msg = sendRequest(RequestMethod.ELICITATION_CREATE, ElicitRequest.CODEC.toJson(req));
        if (msg instanceof JsonRpcResponse resp) {
            ElicitResult er = ElicitResult.CODEC.fromJson(resp.result());
            if (er.action() == ElicitationAction.ACCEPT) {
                ValidationUtil.validateSchema(req.requestedSchema(), er.content());
            }
            return er;
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    public CreateMessageResponse createMessage(CreateMessageRequest req) throws IOException {
        requireClientCapability(ClientCapability.SAMPLING);
        samplingAccess.requireAllowed(principal);
        try {
            return sampling.createMessage(req, Timeouts.DEFAULT_TIMEOUT_MS);
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
            CreateMessageRequest cmr = CreateMessageRequest.CODEC.fromJson(params);
            CreateMessageResponse resp = createMessage(cmr);
            return new JsonRpcResponse(req.id(), CreateMessageResponse.CODEC.toJson(resp));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        lifecycle.shutdown();
        CloseUtil.closeQuietly(resourceFeature);
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
