package com.amannmalik.mcp.server;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.client.elicitation.ElicitCodec;
import com.amannmalik.mcp.client.elicitation.ElicitRequest;
import com.amannmalik.mcp.client.elicitation.ElicitResult;
import com.amannmalik.mcp.client.elicitation.ElicitationAction;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.client.roots.RootsListener;
import com.amannmalik.mcp.client.roots.RootsSubscription;
import com.amannmalik.mcp.client.sampling.CreateMessageRequest;
import com.amannmalik.mcp.client.sampling.CreateMessageResponse;
import com.amannmalik.mcp.client.sampling.SamplingCodec;
import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.jsonrpc.IdTracker;
import com.amannmalik.mcp.jsonrpc.JsonRpcCodec;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.lifecycle.ClientCapability;
import com.amannmalik.mcp.lifecycle.InitializeRequest;
import com.amannmalik.mcp.lifecycle.InitializeResponse;
import com.amannmalik.mcp.lifecycle.LifecycleCodec;
import com.amannmalik.mcp.lifecycle.LifecycleState;
import com.amannmalik.mcp.lifecycle.ProtocolLifecycle;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.lifecycle.ServerFeatures;
import com.amannmalik.mcp.lifecycle.ServerInfo;
import com.amannmalik.mcp.lifecycle.UnsupportedProtocolVersionException;
import com.amannmalik.mcp.lifecycle.Protocol;
import com.amannmalik.mcp.ping.PingCodec;
import com.amannmalik.mcp.ping.PingRequest;
import com.amannmalik.mcp.prompts.GetPromptRequest;
import com.amannmalik.mcp.prompts.InMemoryPromptProvider;
import com.amannmalik.mcp.prompts.ListPromptsRequest;
import com.amannmalik.mcp.prompts.Prompt;
import com.amannmalik.mcp.prompts.PromptArgument;
import com.amannmalik.mcp.prompts.PromptCodec;
import com.amannmalik.mcp.prompts.PromptContent;
import com.amannmalik.mcp.prompts.PromptInstance;
import com.amannmalik.mcp.prompts.PromptMessageTemplate;
import com.amannmalik.mcp.prompts.PromptProvider;
import com.amannmalik.mcp.prompts.PromptTemplate;
import com.amannmalik.mcp.prompts.PromptsSubscription;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.security.PrivacyBoundaryEnforcer;
import com.amannmalik.mcp.security.RateLimiter;
import com.amannmalik.mcp.security.ResourceAccessController;
import com.amannmalik.mcp.security.SamplingAccessPolicy;
import com.amannmalik.mcp.security.ToolAccessPolicy;
import com.amannmalik.mcp.server.completion.CompleteRequest;
import com.amannmalik.mcp.server.completion.CompleteResult;
import com.amannmalik.mcp.server.completion.CompletionCodec;
import com.amannmalik.mcp.server.completion.CompletionProvider;
import com.amannmalik.mcp.server.completion.InMemoryCompletionProvider;
import com.amannmalik.mcp.server.logging.LoggingCodec;
import com.amannmalik.mcp.server.logging.LoggingLevel;
import com.amannmalik.mcp.server.logging.LoggingMessageNotification;
import com.amannmalik.mcp.server.resources.InMemoryResourceProvider;
import com.amannmalik.mcp.server.resources.ListResourceTemplatesRequest;
import com.amannmalik.mcp.server.resources.ListResourceTemplatesResult;
import com.amannmalik.mcp.server.resources.ListResourcesRequest;
import com.amannmalik.mcp.server.resources.ListResourcesResult;
import com.amannmalik.mcp.server.resources.ReadResourceRequest;
import com.amannmalik.mcp.server.resources.ReadResourceResult;
import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.server.resources.ResourceListSubscription;
import com.amannmalik.mcp.server.resources.ResourceProvider;
import com.amannmalik.mcp.server.resources.ResourceSubscription;
import com.amannmalik.mcp.server.resources.ResourceListChangedNotification;
import com.amannmalik.mcp.server.resources.ResourceTemplate;
import com.amannmalik.mcp.server.resources.ResourceUpdatedNotification;
import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.server.resources.SubscribeRequest;
import com.amannmalik.mcp.server.resources.UnsubscribeRequest;
import com.amannmalik.mcp.server.tools.CallToolRequest;
import com.amannmalik.mcp.server.tools.InMemoryToolProvider;
import com.amannmalik.mcp.server.tools.ListToolsRequest;
import com.amannmalik.mcp.server.tools.Tool;
import com.amannmalik.mcp.server.tools.ToolCodec;
import com.amannmalik.mcp.server.tools.ToolListChangedNotification;
import com.amannmalik.mcp.server.tools.ToolListSubscription;
import com.amannmalik.mcp.server.tools.ToolProvider;
import com.amannmalik.mcp.server.tools.ToolResult;
import com.amannmalik.mcp.prompts.PromptListChangedNotification;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.CancellationCodec;
import com.amannmalik.mcp.util.CancellationTracker;
import com.amannmalik.mcp.util.CancelledNotification;
import com.amannmalik.mcp.util.CloseUtil;
import com.amannmalik.mcp.util.Pagination;
import com.amannmalik.mcp.util.ProgressManager;
import com.amannmalik.mcp.util.ProgressNotification;
import com.amannmalik.mcp.util.ProgressToken;
import com.amannmalik.mcp.util.ProgressUtil;
import com.amannmalik.mcp.util.JsonRpcRequestProcessor;
import com.amannmalik.mcp.util.ListChangeListener;
import com.amannmalik.mcp.util.ListChangeSubscription;
import com.amannmalik.mcp.util.Timeouts;
import com.amannmalik.mcp.util.RootChecker;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.SchemaValidator;
import com.amannmalik.mcp.wire.NotificationMethod;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParsingException;

import java.io.EOFException;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public final class McpServer implements AutoCloseable {
    private final Transport transport;
    private final ProtocolLifecycle lifecycle;
    private final Map<RequestMethod, RequestHandler> requestHandlers = new EnumMap<>(RequestMethod.class);
    private final Map<NotificationMethod, NotificationHandler> notificationHandlers = new EnumMap<>(NotificationMethod.class);
    private final ProgressManager progressManager = new ProgressManager(new RateLimiter(20, 1000));
    private final CancellationTracker cancellationTracker = new CancellationTracker();
    private final IdTracker idTracker = new IdTracker();
    private final JsonRpcRequestProcessor requestProcessor =
            new JsonRpcRequestProcessor(progressManager, cancellationTracker, this::send, idTracker);
    private final ResourceProvider resources;
    private final ToolProvider tools;
    private final PromptProvider prompts;
    private final CompletionProvider completions;
    private final Map<String, ResourceSubscription> resourceSubscriptions = new ConcurrentHashMap<>();
    private ResourceListSubscription resourceListSubscription;
    private ToolListSubscription toolListSubscription;
    private PromptsSubscription promptsSubscription;
    private final boolean toolListChangedSupported;
    private final boolean resourcesSubscribeSupported;
    private final boolean resourcesListChangedSupported;
    private final boolean promptsListChangedSupported;
    private final RootsManager rootsManager;
    private final ResourceAccessController resourceAccess;
    private final ToolAccessPolicy toolAccess;
    private final SamplingAccessPolicy samplingAccess;
    private final Principal principal;
    private volatile LoggingLevel logLevel = LoggingLevel.INFO;
    private static final int RATE_LIMIT_CODE = -32001;
    private final RateLimiter toolLimiter = new RateLimiter(5, 1000);
    private final RateLimiter completionLimiter = new RateLimiter(10, 1000);
    private final RateLimiter logLimiter = new RateLimiter(20, 1000);
    private final AtomicLong requestCounter = new AtomicLong(1);
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();

    public McpServer(Transport transport, String instructions) {
        this(createDefaultResources(), createDefaultTools(), createDefaultPrompts(), createDefaultCompletions(),
                createDefaultPrivacyBoundary("default"),
                createDefaultToolAccess(),
                createDefaultSamplingAccess(),
                new Principal("default", Set.of()),
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
        this.lifecycle = new ProtocolLifecycle(caps, new ServerInfo("mcp-java", "MCP Java Reference", "0.1.0"), instructions);
        this.resources = resources;
        this.tools = tools;
        this.prompts = prompts;
        this.completions = completions;
        this.resourceAccess = resourceAccess;
        this.toolAccess = toolAccess == null ? ToolAccessPolicy.PERMISSIVE : toolAccess;
        this.samplingAccess = samplingAccess == null ? SamplingAccessPolicy.PERMISSIVE : samplingAccess;
        this.principal = principal;
        this.toolListChangedSupported = tools != null && tools.supportsListChanged();
        this.resourcesSubscribeSupported = resources != null && resources.supportsSubscribe();
        this.resourcesListChangedSupported = resources != null && resources.supportsListChanged();
        this.promptsListChangedSupported = prompts != null && prompts.supportsListChanged();
        this.rootsManager = new RootsManager(lifecycle, this::sendRequest);

        if (resources != null && resourcesListChangedSupported) {
            resourceListSubscription = subscribeListChanges(
                    l -> resources.subscribeList(() -> l.listChanged()),
                    NotificationMethod.RESOURCES_LIST_CHANGED,
                    ResourcesCodec.toJsonObject(new ResourceListChangedNotification()));
        }

        if (tools != null && toolListChangedSupported) {
            toolListSubscription = subscribeListChanges(
                    l -> tools.subscribeList(() -> l.listChanged()),
                    NotificationMethod.TOOLS_LIST_CHANGED,
                    ToolCodec.toJsonObject(new ToolListChangedNotification()));
        }

        if (prompts != null && promptsListChangedSupported) {
            promptsSubscription = subscribeListChanges(
                    l -> prompts.subscribe(() -> l.listChanged()),
                    NotificationMethod.PROMPTS_LIST_CHANGED,
                    PromptCodec.toJsonObject(new PromptListChangedNotification()));
        }

        registerRequestHandler(RequestMethod.INITIALIZE, this::initialize);
        registerNotificationHandler(NotificationMethod.INITIALIZED, this::initialized);
        registerRequestHandler(RequestMethod.PING, this::ping);
        registerNotificationHandler(NotificationMethod.CANCELLED, this::cancelled);
        registerNotificationHandler(NotificationMethod.ROOTS_LIST_CHANGED, n -> rootsManager.listChangedNotification());

        if (resources != null) {
            registerRequestHandler(RequestMethod.RESOURCES_LIST, this::listResources);
            registerRequestHandler(RequestMethod.RESOURCES_READ, this::readResource);
            registerRequestHandler(RequestMethod.RESOURCES_TEMPLATES_LIST, this::listTemplates);
            if (resourcesSubscribeSupported) {
                registerRequestHandler(RequestMethod.RESOURCES_SUBSCRIBE, this::subscribeResource);
                registerRequestHandler(RequestMethod.RESOURCES_UNSUBSCRIBE, this::unsubscribeResource);
            }
        }

        if (tools != null) {
            registerRequestHandler(RequestMethod.TOOLS_LIST, this::listTools);
            registerRequestHandler(RequestMethod.TOOLS_CALL, this::callTool);
        }

        if (prompts != null) {
            registerRequestHandler(RequestMethod.PROMPTS_LIST, this::listPrompts);
            registerRequestHandler(RequestMethod.PROMPTS_GET, this::getPrompt);
        }

        registerRequestHandler(RequestMethod.LOGGING_SET_LEVEL, this::setLogLevel);

        if (completions != null) {
            registerRequestHandler(RequestMethod.COMPLETION_COMPLETE, this::complete);
        }

        registerRequestHandler(RequestMethod.SAMPLING_CREATE_MESSAGE, this::handleCreateMessage);
    }

    private void registerRequestHandler(RequestMethod method, RequestHandler handler) {
        requestHandlers.put(method, handler);
    }

    private void registerNotificationHandler(NotificationMethod method, NotificationHandler handler) {
        notificationHandlers.put(method, handler);
    }

    private <S extends ListChangeSubscription> S subscribeListChanges(
            SubscriptionFactory<S> factory,
            NotificationMethod method,
            jakarta.json.JsonObject payload) {
        try {
            return factory.subscribe(() -> {
                if (lifecycle.state() != LifecycleState.OPERATION) return;
                try {
                    send(new JsonRpcNotification(method.method(), payload));
                } catch (IOException ignore) {
                }
            });
        } catch (Exception ignore) {
            return null;
        }
    }

    @FunctionalInterface
    private interface SubscriptionFactory<S extends ListChangeSubscription> {
        S subscribe(ListChangeListener listener) throws Exception;
    }

    public void serve() throws IOException {
        while (lifecycle.state() != LifecycleState.SHUTDOWN) {
            JsonObject obj;
            try {
                obj = transport.receive();
            } catch (EOFException e) {
                lifecycle.shutdown();
                break;
            } catch (JsonParsingException e) {
                System.err.println("Parse error: " + e.getMessage());
                sendLog(LoggingLevel.ERROR, "parser", Json.createValue(e.getMessage()));
                try {
                    send(JsonRpcError.of(new RequestId.NullId(), JsonRpcErrorCode.PARSE_ERROR, e.getMessage()));
                } catch (IOException ioe) {
                    System.err.println("Failed to send error: " + ioe.getMessage());
                }
                continue;
            }

            try {
                JsonRpcMessage msg = JsonRpcCodec.fromJsonObject(obj);
                switch (msg) {
                    case JsonRpcRequest req -> onRequest(req);
                    case JsonRpcNotification note -> onNotification(note);
                    case JsonRpcResponse resp -> {
                        CompletableFuture<JsonRpcMessage> f = pending.remove(resp.id());
                        if (f != null) f.complete(resp);
                    }
                    case JsonRpcError err -> {
                        CompletableFuture<JsonRpcMessage> f = pending.remove(err.id());
                        if (f != null) f.complete(err);
                    }
                    default -> {
                    }
                }
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid request: " + e.getMessage());
                sendLog(LoggingLevel.WARNING, "server", Json.createValue(e.getMessage()));
                try {
                    send(JsonRpcError.of(new RequestId.NullId(), JsonRpcErrorCode.INVALID_REQUEST, e.getMessage()));
                } catch (IOException ioe) {
                    System.err.println("Failed to send error: " + ioe.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error processing message: " + e.getMessage());
                sendLog(LoggingLevel.ERROR, "server", Json.createValue(e.getMessage()));
            } catch (Exception e) {
                System.err.println("Unexpected error processing message: " + e.getMessage());
                sendLog(LoggingLevel.ERROR, "server", Json.createValue(e.getMessage()));
            }
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

        var method = RequestMethod.from(req.method());
        if (method.isEmpty()) {
            send(JsonRpcError.of(req.id(),
                    JsonRpcErrorCode.METHOD_NOT_FOUND,
                    "Unknown method: " + req.method()));
            return;
        }

        RequestHandler handler = requestHandlers.get(method.get());
        if (handler == null) {
            send(JsonRpcError.of(req.id(),
                    JsonRpcErrorCode.METHOD_NOT_FOUND,
                    "Unknown method: " + req.method()));
            return;
        }

        boolean cancellable = method.get() != RequestMethod.INITIALIZE;
        var resp = requestProcessor.process(req, cancellable, handler::handle);
        if (resp.isPresent()) send(resp.get());
    }

    private void onNotification(JsonRpcNotification note) throws IOException {
        NotificationMethod.from(note.method())
                .map(notificationHandlers::get)
                .ifPresent(h -> {
                    try {
                        h.handle(note);
                    } catch (IOException ignore) {
                    }
                });
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
                new ServerFeatures(
                        resourcesSubscribeSupported,
                        resourcesListChangedSupported,
                        toolListChangedSupported,
                        promptsListChangedSupported
                )
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
        ListResourcesRequest lr = ResourcesCodec.toListResourcesRequest(req.params());
        String cursor;
        try {
            cursor = sanitizeCursor(lr.cursor());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }

        Pagination.Page<Resource> list;
        try {
            list = resources.list(cursor);
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }

        List<Resource> filteredResources = list.items().stream()
                .filter(r -> allowed(r.annotations()) && withinRoots(r.uri()))
                .toList();

        ListResourcesResult result = new ListResourcesResult(filteredResources, list.nextCursor(), null);
        JsonObject resultJson = ResourcesCodec.toJsonObject(result);
        return new JsonRpcResponse(req.id(), resultJson);
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        ReadResourceRequest rrr;
        try {
            rrr = ResourcesCodec.toReadResourceRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
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
        ListResourceTemplatesRequest request;
        try {
            request = ResourcesCodec.toListResourceTemplatesRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }

        String cursor;
        try {
            cursor = sanitizeCursor(request.cursor());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }

        Pagination.Page<ResourceTemplate> page;
        try {
            page = resources.listTemplates(cursor);
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }

        List<ResourceTemplate> filteredTemplates = page.items().stream()
                .filter(t -> allowed(t.annotations()))
                .toList();

        ListResourceTemplatesResult result = new ListResourceTemplatesResult(filteredTemplates, page.nextCursor(), null);
        JsonObject resultJson = ResourcesCodec.toJsonObject(result);
        return new JsonRpcResponse(req.id(), resultJson);
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        SubscribeRequest sr;
        try {
            sr = ResourcesCodec.toSubscribeRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
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
            ur = ResourcesCodec.toUnsubscribeRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
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
        ListToolsRequest ltr = ToolCodec.toListToolsRequest(req.params());
        String cursor;
        try {
            cursor = sanitizeCursor(ltr.cursor());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        Pagination.Page<Tool> page;
        try {
            page = tools.list(cursor);
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        JsonObject result = ToolCodec.toJsonObject(page, null);
        return new JsonRpcResponse(req.id(), result);
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
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        ListPromptsRequest lpr = PromptCodec.toListPromptsRequest(req.params());
        String cursor;
        try {
            cursor = sanitizeCursor(lpr.cursor());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        Pagination.Page<Prompt> page;
        try {
            page = prompts.list(cursor);
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        JsonObject result = PromptCodec.toJsonObject(page, null);
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        GetPromptRequest getRequest;
        try {
            getRequest = PromptCodec.toGetPromptRequest(req.params());
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
        try {
            PromptInstance inst = prompts.get(getRequest.name(), getRequest.arguments());
            JsonObject result = PromptCodec.toJsonObject(inst);
            return new JsonRpcResponse(req.id(), result);
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
        }
    }

    private JsonRpcMessage setLogLevel(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.LOGGING);
        JsonObject params = req.params();
        if (params == null) {
            return invalidParams(req, "Missing params");
        }
        try {
            logLevel = LoggingCodec.toSetLevelRequest(params).level();
            return new JsonRpcResponse(req.id(), JsonValue.EMPTY_JSON_OBJECT);
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
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
            CompleteRequest request = CompletionCodec.toCompleteRequest(params);
            Optional<String> limit = rateLimit(completionLimiter, request.ref().toString());
            if (limit.isPresent()) {
                return JsonRpcError.of(req.id(), RATE_LIMIT_CODE, limit.get());
            }
            CompleteResult result = completions.complete(request);
            return new JsonRpcResponse(req.id(), CompletionCodec.toJsonObject(result));
        } catch (IllegalArgumentException e) {
            return invalidParams(req, e);
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

    public RootsSubscription subscribeRoots(RootsListener listener) {
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

    private static ResourceProvider createDefaultResources() {
        Resource r = new Resource("test://example", "example", null, null, "text/plain", 5L, null, null);
        ResourceBlock.Text block = new ResourceBlock.Text("test://example", "text/plain", "hello", null);
        ResourceTemplate t = new ResourceTemplate("test://template", "example_template", null, null, "text/plain", null, null);
        return new InMemoryResourceProvider(List.of(r), Map.of(r.uri(), block), List.of(t));
    }

    private static ToolProvider createDefaultTools() {
        var schema = Json.createObjectBuilder().add("type", "object").build();
        Tool tool = new Tool("test_tool", "Test Tool", null, schema, null, null, null);
        return new InMemoryToolProvider(
                List.of(tool),
                Map.of("test_tool", a -> new ToolResult(
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("type", "text")
                                        .add("text", "ok")
                                        .build())
                                .build(), null, false, null)));
    }

    private static PromptProvider createDefaultPrompts() {
        InMemoryPromptProvider p = new InMemoryPromptProvider();
        PromptArgument arg = new PromptArgument("test_arg", null, null, true, null);
        Prompt prompt = new Prompt("test_prompt", "Test Prompt", null, List.of(arg), null);
        PromptMessageTemplate msg = new PromptMessageTemplate(Role.USER, new ContentBlock.Text("hello", null, null));
        p.add(new PromptTemplate(prompt, List.of(msg)));
        return p;
    }

    private static CompletionProvider createDefaultCompletions() {
        InMemoryCompletionProvider provider = new InMemoryCompletionProvider();
        provider.add(new CompleteRequest.Ref.PromptRef("test_prompt", null, null), "test_arg", Map.of(), List.of("test_completion"));
        return provider;
    }

    private static ToolAccessPolicy createDefaultToolAccess() {
        return ToolAccessPolicy.PERMISSIVE;
    }

    private static SamplingAccessPolicy createDefaultSamplingAccess() {
        return SamplingAccessPolicy.PERMISSIVE;
    }

    private static ResourceAccessController createDefaultPrivacyBoundary(String principalId) {
        var p = new PrivacyBoundaryEnforcer();
        for (Role a : Role.values()) p.allow(principalId, a);
        return p;
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

    @FunctionalInterface
    protected interface RequestHandler {
        JsonRpcMessage handle(JsonRpcRequest request);
    }

    @FunctionalInterface
    protected interface NotificationHandler {
        void handle(JsonRpcNotification notification) throws IOException;
    }
}
