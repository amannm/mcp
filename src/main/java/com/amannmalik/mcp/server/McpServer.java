package com.amannmalik.mcp.server;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.client.elicitation.*;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.client.roots.RootsListener;
import com.amannmalik.mcp.client.sampling.*;
import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.*;
import com.amannmalik.mcp.ping.PingCodec;
import com.amannmalik.mcp.ping.PingRequest;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.resources.*;
import com.amannmalik.mcp.security.*;
import com.amannmalik.mcp.server.completion.*;
import com.amannmalik.mcp.server.logging.*;
import com.amannmalik.mcp.server.roots.RootsManager;
import com.amannmalik.mcp.server.tools.*;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.*;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.SchemaValidator;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.*;
import jakarta.json.stream.JsonParsingException;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class McpServer implements AutoCloseable {
    private final Transport transport;
    private final ProtocolLifecycle lifecycle;
    private final RpcHandlerRegistry handlers;
    private final ProgressManager progressManager = new ProgressManager(
            new RateLimiter(McpConfiguration.current().performance().rateLimits().progressPerSecond(), 1000));
    private final CancellationTracker cancellationTracker = new CancellationTracker();
    private final IdTracker idTracker = new IdTracker();
    private final ResourceProvider resources;
    private final ToolProvider tools;
    private final PromptProvider prompts;
    private final CompletionProvider completions;
    private final Map<String, ResourceSubscription> resourceSubscriptions = new ConcurrentHashMap<>();
    private ListChangeSubscription resourceListSubscription;
    private ListChangeSubscription toolListSubscription;
    private ListChangeSubscription promptsSubscription;
    private final RootsManager rootsManager;
    private final ResourceAccessController resourceAccess;
    private final ToolAccessPolicy toolAccess;
    private final SamplingAccessPolicy samplingAccess;
    private final Principal principal;
    private volatile LoggingLevel logLevel = LoggingLevel.INFO;
    private static final int RATE_LIMIT_CODE = -32001;
    private final RateLimiter toolLimiter = new RateLimiter(
            McpConfiguration.current().performance().rateLimits().toolsPerSecond(), 1000);
    private final RateLimiter completionLimiter = new RateLimiter(
            McpConfiguration.current().performance().rateLimits().completionsPerSecond(), 1000);
    private final RateLimiter logLimiter = new RateLimiter(
            McpConfiguration.current().performance().rateLimits().logsPerSecond(), 1000);
    private final AtomicLong requestCounter = new AtomicLong(1);
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();

    public McpServer(Transport transport, String instructions) {
        this(ServerDefaults.resources(),
                ServerDefaults.tools(),
                ServerDefaults.prompts(),
                ServerDefaults.completions(),
                ServerDefaults.privacyBoundary("default"),
                ServerDefaults.toolAccess(),
                ServerDefaults.samplingAccess(),
                new Principal(McpConfiguration.current().security().auth().defaultPrincipal(), Set.of()),
                instructions,
                transport);
    }

    McpServer(ResourceProvider resources,
              ToolProvider tools,
              PromptProvider prompts,
              CompletionProvider completions,
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
                McpConfiguration.current().server().info().name(),
                McpConfiguration.current().server().info().description(),
                McpConfiguration.current().server().info().version()), instructions);
        this.resources = resources;
        this.tools = tools;
        this.prompts = prompts;
        this.completions = completions;
        this.resourceAccess = resourceAccess;
        this.toolAccess = toolAccess == null ? ToolAccessPolicy.PERMISSIVE : toolAccess;
        this.samplingAccess = samplingAccess == null ? SamplingAccessPolicy.PERMISSIVE : samplingAccess;
        var requestProcessor = new JsonRpcRequestProcessor(progressManager, cancellationTracker, this::send, idTracker);
        this.handlers = new RpcHandlerRegistry(requestProcessor);
        this.principal = principal;
        this.rootsManager = new RootsManager(lifecycle, this::sendRequest);

        if (resources != null && resources.supportsListChanged()) {
            resourceListSubscription = subscribeListChanges(
                    l -> resources.subscribeList(() -> l.listChanged()),
                    NotificationMethod.RESOURCES_LIST_CHANGED,
                    ResourcesCodec.toJsonObject(new ResourceListChangedNotification()));
        }

        if (tools != null && tools.supportsListChanged()) {
            toolListSubscription = subscribeListChanges(
                    l -> tools.subscribeList(() -> l.listChanged()),
                    NotificationMethod.TOOLS_LIST_CHANGED,
                    ToolCodec.toJsonObject(new ToolListChangedNotification()));
        }

        if (prompts != null && prompts.supportsListChanged()) {
            promptsSubscription = subscribeListChanges(
                    l -> prompts.subscribe(() -> l.listChanged()),
                    NotificationMethod.PROMPTS_LIST_CHANGED,
                    PromptCodec.toJsonObject(new PromptListChangedNotification()));
        }

        handlers.register(RequestMethod.INITIALIZE, this::initialize);
        handlers.register(NotificationMethod.INITIALIZED, this::initialized);
        handlers.register(RequestMethod.PING, this::ping);
        handlers.register(NotificationMethod.CANCELLED, this::cancelled);
        handlers.register(NotificationMethod.ROOTS_LIST_CHANGED, n -> rootsManager.listChangedNotification());

        if (resources != null) {
            handlers.register(RequestMethod.RESOURCES_LIST, this::listResources);
            handlers.register(RequestMethod.RESOURCES_READ, this::readResource);
            handlers.register(RequestMethod.RESOURCES_TEMPLATES_LIST, this::listTemplates);
            if (resources.supportsSubscribe()) {
                handlers.register(RequestMethod.RESOURCES_SUBSCRIBE, this::subscribeResource);
                handlers.register(RequestMethod.RESOURCES_UNSUBSCRIBE, this::unsubscribeResource);
            }
        }

        if (tools != null) {
            handlers.register(RequestMethod.TOOLS_LIST, this::listTools);
            handlers.register(RequestMethod.TOOLS_CALL, this::callTool);
        }

        if (prompts != null) {
            handlers.register(RequestMethod.PROMPTS_LIST, this::listPrompts);
            handlers.register(RequestMethod.PROMPTS_GET, this::getPrompt);
        }

        handlers.register(RequestMethod.LOGGING_SET_LEVEL, this::setLogLevel);

        if (completions != null) {
            handlers.register(RequestMethod.COMPLETION_COMPLETE, this::complete);
        }

        handlers.register(RequestMethod.SAMPLING_CREATE_MESSAGE, this::handleCreateMessage);
    }

    private <S extends ListChangeSubscription> S subscribeListChanges(
            SubscriptionFactory<S> factory,
            NotificationMethod method,
            JsonObject payload) {
        try {
            return factory.subscribe(() -> {
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
    private interface SubscriptionFactory<S extends ListChangeSubscription> {
        S subscribe(ListChangeListener listener);
    }

    public void serve() throws IOException {
        while (lifecycle.state() != LifecycleState.SHUTDOWN) {
            var obj = receiveMessage();
            if (obj.isEmpty()) continue;
            try {
                processMessage(JsonRpcCodec.fromJsonObject(obj.get()));
            } catch (IllegalArgumentException e) {
                handleInvalidRequest(e);
            } catch (IOException e) {
                System.err.println("Error processing message: " + e.getMessage());
                sendLog(LoggingLevel.ERROR, "server", Json.createValue(e.getMessage()));
            } catch (Exception e) {
                System.err.println("Unexpected error processing message: " + e.getMessage());
                sendLog(LoggingLevel.ERROR, "server", Json.createValue(e.getMessage()));
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
        System.err.println("Parse error: " + e.getMessage());
        try {
            sendLog(LoggingLevel.ERROR, "parser", Json.createValue(e.getMessage()));
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
        System.err.println("Invalid request: " + e.getMessage());
        try {
            sendLog(LoggingLevel.WARNING, "server", Json.createValue(e.getMessage()));
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
                    "Server not initialized",
                    null));
            return;
        }

        boolean cancellable = RequestMethod.from(req.method()).map(m -> m != RequestMethod.INITIALIZE).orElse(true);
        var resp = handlers.handle(req, cancellable);
        if (resp.isPresent()) send(resp.get());
    }

    private void onNotification(JsonRpcNotification note) {
        handlers.handle(note);
    }

    private JsonRpcMessage initialize(JsonRpcRequest req) {
        InitializeRequest init = LifecycleCodec.toInitializeRequest(req.params());
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
        var json = LifecycleCodec.toJsonObject(resp);
        return new JsonRpcResponse(req.id(), json);
    }

    private void initialized(JsonRpcNotification ignored) {
        lifecycle.initialized();
        rootsManager.refreshAsync();
    }

    private JsonRpcMessage ping(JsonRpcRequest req) {
        PingRequest ignored = PingCodec.toPingRequest(req);
        return PingCodec.toResponse(req.id());
    }

    private synchronized void send(JsonRpcMessage msg) throws IOException {
        transport.send(JsonRpcCodec.toJsonObject(msg));
    }

    private void requireClientCapability(ClientCapability cap) {
        if (!lifecycle.negotiatedClientCapabilities().contains(cap)) {
            throw new IllegalStateException("Missing client capability: " + cap);
        }
    }

    private ServerFeatures serverFeatures() {
        return new ServerFeatures(
                resources != null && resources.supportsSubscribe(),
                resources != null && resources.supportsListChanged(),
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

    private boolean withinRoots(String uri) {
        return RootChecker.withinRoots(uri, rootsManager.roots());
    }

    private boolean canAccessResource(String uri) {
        if (!withinRoots(uri)) return false;
        return resources.get(uri)
                .map(Resource::annotations)
                .map(this::allowed)
                .orElse(true);
    }

    private JsonRpcError invalidParams(JsonRpcRequest req, String message) {
        return JsonRpcError.invalidParams(req.id(), message);
    }

    private JsonRpcError invalidParams(JsonRpcRequest req, IllegalArgumentException e) {
        return invalidParams(req, e.getMessage());
    }

    private <T> T valid(Supplier<T> s) {
        try {
            return s.get();
        } catch (IllegalArgumentException e) {
            throw new InvalidParams(e.getMessage());
        }
    }

    private static final class InvalidParams extends RuntimeException {
        InvalidParams(String message) {
            super(message);
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
        CancelledNotification cn = CancellationCodec.toCancelledNotification(note.params());
        cancellationTracker.cancel(cn.requestId(), cn.reason());
        progressManager.release(cn.requestId());
        try {
            String reason = cancellationTracker.reason(cn.requestId());
            sendLog(LoggingLevel.INFO, "cancellation",
                    reason == null ? JsonValue.NULL : Json.createValue(reason));
        } catch (IOException ignore) {
        }
    }

    private String sanitizeCursor(String cursor) {
        return cursor == null ? null : Pagination.sanitize(InputSanitizer.cleanNullable(cursor));
    }

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        try {
            ListResourcesRequest lr = valid(() -> ResourcesCodec.toListResourcesRequest(req.params()));
            String cursor = valid(() -> sanitizeCursor(lr.cursor()));
            Pagination.Page<Resource> list = valid(() -> resources.list(cursor));
            List<Resource> filtered = list.items().stream()
                    .filter(r -> allowed(r.annotations()) && withinRoots(r.uri()))
                    .toList();
            ListResourcesResult result = new ListResourcesResult(filtered, list.nextCursor(), null);
            return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        ReadResourceRequest rrr;
        try {
            rrr = valid(() -> ResourcesCodec.toReadResourceRequest(req.params()));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
        String uri = rrr.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceBlock block = resources.read(uri);
        if (block == null) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found",
                    Json.createObjectBuilder().add("uri", uri).build());
        }
        ReadResourceResult result = new ReadResourceResult(List.of(block), null);
        return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        try {
            ListResourceTemplatesRequest request = valid(() -> ResourcesCodec.toListResourceTemplatesRequest(req.params()));
            String cursor = valid(() -> sanitizeCursor(request.cursor()));
            Pagination.Page<ResourceTemplate> page = valid(() -> resources.listTemplates(cursor));
            List<ResourceTemplate> filtered = page.items().stream()
                    .filter(t -> allowed(t.annotations()))
                    .toList();
            ListResourceTemplatesResult result = new ListResourceTemplatesResult(filtered, page.nextCursor(), null);
            return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        SubscribeRequest sr;
        try {
            sr = valid(() -> ResourcesCodec.toSubscribeRequest(req.params()));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
        String uri = sr.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceBlock existing = resources.read(uri);
        if (existing == null) {
            return JsonRpcError.of(req.id(), -32002, "Resource not found",
                    Json.createObjectBuilder().add("uri", uri).build());
        }
        try {
            ResourceSubscription sub = resources.subscribe(uri, update -> {
                try {
                    ResourceUpdatedNotification n = new ResourceUpdatedNotification(update.uri(), update.title());
                    send(new JsonRpcNotification(
                            NotificationMethod.RESOURCES_UPDATED.method(),
                            ResourcesCodec.toJsonObject(n)));
                } catch (IOException ignore) {
                }
            });
            ResourceSubscription prev = resourceSubscriptions.put(uri, sub);
            CloseUtil.closeQuietly(prev);
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
        return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
    }

    private JsonRpcMessage unsubscribeResource(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        UnsubscribeRequest ur;
        try {
            ur = valid(() -> ResourcesCodec.toUnsubscribeRequest(req.params()));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
        String uri = ur.uri();
        if (!canAccessResource(uri)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        ResourceSubscription sub = resourceSubscriptions.remove(uri);
        CloseUtil.closeQuietly(sub);
        return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
    }

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.TOOLS);
        try {
            ListToolsRequest ltr = valid(() -> ToolCodec.toListToolsRequest(req.params()));
            String cursor = valid(() -> sanitizeCursor(ltr.cursor()));
            Pagination.Page<Tool> page = valid(() -> tools.list(cursor));
            return new JsonRpcResponse(req.id(), ToolCodec.toJsonObject(page, null));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
    }

    private JsonRpcMessage callTool(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.TOOLS);
        CallToolRequest callRequest;
        try {
            callRequest = ToolCodec.toCallToolRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        Optional<String> limit = rateLimit(toolLimiter, callRequest.name());
        if (limit.isPresent()) {
            return JsonRpcError.of(req.id(), RATE_LIMIT_CODE, limit.get());
        }
        try {
            toolAccess.requireAllowed(principal, callRequest.name());
        } catch (SecurityException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, "Access denied");
        }
        try {
            ToolResult result = tools.call(callRequest.name(), callRequest.arguments());
            return new JsonRpcResponse(req.id(), ToolCodec.toJsonObject(result));
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
                    ToolResult result = tools.call(callRequest.name(), res.content());
                    return new JsonRpcResponse(req.id(), ToolCodec.toJsonObject(result));
                }
                return invalidParams(req, "Tool invocation cancelled");
            } catch (Exception ex) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, ex.getMessage());
            }
        }
        return invalidParams(req, e);
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            ListPromptsRequest lpr = valid(() -> PromptCodec.toListPromptsRequest(req.params()));
            String cursor = valid(() -> sanitizeCursor(lpr.cursor()));
            Pagination.Page<Prompt> page = valid(() -> prompts.list(cursor));
            return new JsonRpcResponse(req.id(), PromptCodec.toJsonObject(page, null));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        try {
            GetPromptRequest getRequest = valid(() -> PromptCodec.toGetPromptRequest(req.params()));
            PromptInstance inst = valid(() -> prompts.get(getRequest.name(), getRequest.arguments()));
            return new JsonRpcResponse(req.id(), PromptCodec.toJsonObject(inst));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
    }

    private JsonRpcMessage setLogLevel(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.LOGGING);
        JsonObject params = req.params();
        if (params == null) {
            return invalidParams(req, "Missing params");
        }
        try {
            logLevel = valid(() -> LoggingCodec.toSetLevelRequest(params)).level();
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
        }
    }

    private void sendLog(LoggingMessageNotification note) throws IOException {
        if (rateLimit(logLimiter, note.logger() == null ? "" : note.logger()).isPresent() ||
                note.level().ordinal() < logLevel.ordinal()) return;
        requireServerCapability(ServerCapability.LOGGING);
        send(new JsonRpcNotification(NotificationMethod.MESSAGE.method(),
                LoggingCodec.toJsonObject(note)));
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
            return invalidParams(req, "Missing params");
        }
        try {
            CompleteRequest request = valid(() -> CompletionCodec.toCompleteRequest(params));
            Optional<String> limit = rateLimit(completionLimiter, request.ref().toString());
            if (limit.isPresent()) {
                return JsonRpcError.of(req.id(), RATE_LIMIT_CODE, limit.get());
            }
            CompleteResult result = valid(() -> completions.complete(request));
            return new JsonRpcResponse(req.id(), CompletionCodec.toJsonObject(result));
        } catch (InvalidParams e) {
            return invalidParams(req, e.getMessage());
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
        try {
            return future.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        } catch (TimeoutException e) {
            try {
                send(new JsonRpcNotification(
                        NotificationMethod.CANCELLED.method(),
                        CancellationCodec.toJsonObject(new CancelledNotification(id, "timeout"))));
            } catch (IOException ignore) {
            }
            throw new IOException("Request timed out after " + timeoutMillis + " ms");
        } finally {
            pending.remove(id);
        }
    }

    public List<Root> listRoots() throws IOException {
        return rootsManager.listRoots();
    }

    public ListChangeSubscription subscribeRoots(RootsListener listener) {
        return rootsManager.subscribe(listener);
    }

    public List<Root> roots() {
        return rootsManager.roots();
    }

    public ElicitResult elicit(ElicitRequest req) throws IOException {
        requireClientCapability(ClientCapability.ELICITATION);
        JsonRpcMessage msg = sendRequest(RequestMethod.ELICITATION_CREATE, ElicitCodec.toJsonObject(req));
        if (msg instanceof JsonRpcResponse resp) {
            ElicitResult er = ElicitCodec.toResult(resp.result());
            if (er.action() == ElicitationAction.ACCEPT) {
                SchemaValidator.validate(req.requestedSchema(), er.content());
            }
            return er;
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    public CreateMessageResponse createMessage(CreateMessageRequest req) throws IOException {
        requireClientCapability(ClientCapability.SAMPLING);
        samplingAccess.requireAllowed(principal);
        JsonRpcMessage msg = sendRequest(RequestMethod.SAMPLING_CREATE_MESSAGE, SamplingCodec.toJsonObject(req));
        if (msg instanceof JsonRpcResponse resp) {
            return SamplingCodec.toCreateMessageResponse(resp.result());
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private JsonRpcMessage handleCreateMessage(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return invalidParams(req, "Missing params");
        }
        try {
            CreateMessageRequest cmr = SamplingCodec.toCreateMessageRequest(params);
            CreateMessageResponse resp = createMessage(cmr);
            return new JsonRpcResponse(req.id(), SamplingCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }


    @Override
    public void close() throws IOException {
        lifecycle.shutdown();
        for (ResourceSubscription sub : resourceSubscriptions.values()) {
            CloseUtil.closeQuietly(sub);
        }
        resourceSubscriptions.clear();
        if (resourceListSubscription != null) {
            CloseUtil.closeQuietly(resourceListSubscription);
            resourceListSubscription = null;
        }
        if (toolListSubscription != null) {
            CloseUtil.closeQuietly(toolListSubscription);
            toolListSubscription = null;
        }
        if (promptsSubscription != null) {
            CloseUtil.closeQuietly(promptsSubscription);
            promptsSubscription = null;
        }
        if (resources != null) resources.close();
        if (completions != null) completions.close();
        transport.close();
    }
}
