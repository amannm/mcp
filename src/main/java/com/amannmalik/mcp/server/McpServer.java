package com.amannmalik.mcp.server;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.client.elicitation.ElicitCodec;
import com.amannmalik.mcp.client.elicitation.ElicitRequest;
import com.amannmalik.mcp.client.elicitation.ElicitResult;
import com.amannmalik.mcp.client.elicitation.ElicitationAction;
import com.amannmalik.mcp.client.roots.ListRootsRequest;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.client.roots.RootsCodec;
import com.amannmalik.mcp.client.roots.RootsListener;
import com.amannmalik.mcp.client.roots.RootsSubscription;
import com.amannmalik.mcp.client.sampling.CreateMessageRequest;
import com.amannmalik.mcp.client.sampling.CreateMessageResponse;
import com.amannmalik.mcp.client.sampling.SamplingCodec;
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
import com.amannmalik.mcp.prompts.PromptPage;
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
import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.server.resources.ResourceList;
import com.amannmalik.mcp.server.resources.ResourceListSubscription;
import com.amannmalik.mcp.server.resources.ResourceProvider;
import com.amannmalik.mcp.server.resources.ResourceSubscription;
import com.amannmalik.mcp.server.resources.ResourceTemplate;
import com.amannmalik.mcp.server.resources.ResourceTemplatePage;
import com.amannmalik.mcp.server.resources.ResourceUpdatedNotification;
import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.server.resources.SubscribeRequest;
import com.amannmalik.mcp.server.resources.UnsubscribeRequest;
import com.amannmalik.mcp.server.resources.ReadResourceRequest;
import com.amannmalik.mcp.server.resources.ReadResourceResult;
import com.amannmalik.mcp.server.tools.CallToolRequest;
import com.amannmalik.mcp.server.tools.InMemoryToolProvider;
import com.amannmalik.mcp.server.tools.ListToolsRequest;
import com.amannmalik.mcp.server.tools.Tool;
import com.amannmalik.mcp.server.tools.ToolCodec;
import com.amannmalik.mcp.server.tools.ToolListChangedNotification;
import com.amannmalik.mcp.server.tools.ToolListSubscription;
import com.amannmalik.mcp.server.tools.ToolPage;
import com.amannmalik.mcp.server.tools.ToolProvider;
import com.amannmalik.mcp.server.tools.ToolResult;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.CancellationCodec;
import com.amannmalik.mcp.util.CancellationTracker;
import com.amannmalik.mcp.util.CancelledNotification;
import com.amannmalik.mcp.util.ProgressCodec;
import com.amannmalik.mcp.util.ProgressNotification;
import com.amannmalik.mcp.util.ProgressToken;
import com.amannmalik.mcp.util.ProgressTracker;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import com.amannmalik.mcp.validation.SchemaValidator;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParsingException;

import java.io.EOFException;
import java.io.IOException;
import java.util.EnumSet;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

public final class McpServer implements AutoCloseable {
    private final Transport transport;
    private final ProtocolLifecycle lifecycle;
    private final Map<String, RequestHandler> requestHandlers = new ConcurrentHashMap<>();
    private final Map<String, NotificationHandler> notificationHandlers = new ConcurrentHashMap<>();
    private final ProgressTracker progressTracker = new ProgressTracker();
    private final Map<RequestId, ProgressToken> progressTokens = new ConcurrentHashMap<>();
    private final CancellationTracker cancellationTracker = new CancellationTracker();
    private final IdTracker idTracker = new IdTracker();
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
    private final List<RootsListener> rootsListeners = new CopyOnWriteArrayList<>();
    private final List<Root> roots = new CopyOnWriteArrayList<>();
    private final ResourceAccessController resourceAccess;
    private final ToolAccessPolicy toolAccess;
    private final SamplingAccessPolicy samplingAccess;
    private final Principal principal;
    private volatile LoggingLevel logLevel = LoggingLevel.INFO;
    private static final int RATE_LIMIT_CODE = -32001;
    private static final int NOT_INITIALIZED_CODE = -32000;
    private final RateLimiter toolLimiter = new RateLimiter(5, 1000);
    private final RateLimiter completionLimiter = new RateLimiter(10, 1000);
    private final RateLimiter logLimiter = new RateLimiter(20, 1000);
    private final RateLimiter progressLimiter = new RateLimiter(20, 1000);
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

        if (resources != null && resourcesListChangedSupported) {
            try {
                resourceListSubscription = resources.subscribeList(() -> {
                    try {
                        send(new JsonRpcNotification("notifications/resources/list_changed", null));
                    } catch (IOException ignore) {
                    }
                });
            } catch (Exception ignore) {
            }
        }

        if (tools != null && toolListChangedSupported) {
            try {
                toolListSubscription = tools.subscribeList(() -> {
                    try {
                        send(new JsonRpcNotification(
                                "notifications/tools/list_changed",
                                ToolCodec.toJsonObject(new ToolListChangedNotification())));
                    } catch (IOException ignore) {
                    }
                });
            } catch (Exception ignore) {
            }
        }

        if (prompts != null && promptsListChangedSupported) {
            try {
                promptsSubscription = prompts.subscribe(() -> {
                    try {
                        send(new JsonRpcNotification("notifications/prompts/list_changed", null));
                    } catch (IOException ignore) {
                    }
                });
            } catch (Exception ignore) {
            }
        }

        registerRequestHandler("initialize", this::initialize);
        registerNotificationHandler("notifications/initialized", this::initialized);
        registerRequestHandler("ping", this::ping);
        registerNotificationHandler("notifications/cancelled", this::cancelled);
        registerNotificationHandler("notifications/roots/list_changed", n -> rootsListChanged());

        if (resources != null) {
            registerRequestHandler("resources/list", this::listResources);
            registerRequestHandler("resources/read", this::readResource);
            registerRequestHandler("resources/templates/list", this::listTemplates);
            if (resourcesSubscribeSupported) {
                registerRequestHandler("resources/subscribe", this::subscribeResource);
                registerRequestHandler("resources/unsubscribe", this::unsubscribeResource);
            }
        }

        if (tools != null) {
            registerRequestHandler("tools/list", this::listTools);
            registerRequestHandler("tools/call", this::callTool);
        }

        if (prompts != null) {
            registerRequestHandler("prompts/list", this::listPrompts);
            registerRequestHandler("prompts/get", this::getPrompt);
        }

        registerRequestHandler("logging/setLevel", this::setLogLevel);

        if (completions != null) {
            registerRequestHandler("completion/complete", this::complete);
        }

        registerRequestHandler("sampling/createMessage", this::handleCreateMessage);
    }

    private void registerRequestHandler(String method, RequestHandler handler) {
        requestHandlers.put(method, handler);
    }

    private void registerNotificationHandler(String method, NotificationHandler handler) {
        notificationHandlers.put(method, handler);
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
                !"initialize".equals(req.method()) &&
                !"ping".equals(req.method())) {
            send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    NOT_INITIALIZED_CODE, "Server not initialized", null)));
            return;
        }
        var handler = requestHandlers.get(req.method());
        if (handler == null) {
            send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                    "Unknown method: " + req.method(), null)));
            return;
        }
        ProgressToken token = null;
        boolean cancellable = false;
        try {
            try {
                idTracker.register(req.id());
            } catch (IllegalArgumentException e) {
                send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INVALID_REQUEST.code(), e.getMessage(), null)));
                return;
            }

            try {
                token = parseProgressToken(req.params());
                if (token != null) {
                    progressTracker.register(token);
                    progressTokens.put(req.id(), token);
                    try {
                        sendProgress(new ProgressNotification(token, 0.0, 1.0, null));
                    } catch (IOException ignore) {
                    }
                }
            } catch (IllegalArgumentException e) {
                send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null)));
                return;
            }

            cancellable = !"initialize".equals(req.method());
            if (cancellable) {
                cancellationTracker.register(req.id());
            }

            JsonRpcMessage resp;
            try {
                resp = handler.handle(req);
            } catch (IllegalArgumentException e) {
                send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null)));
                return;
            } catch (Exception e) {
                send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null)));
                return;
            }

            boolean cancelled = cancellable && cancellationTracker.isCancelled(req.id());
            if (!cancelled && resp != null) {
                send(resp);
            }
            if (!cancelled && token != null) {
                try {
                    sendProgress(new ProgressNotification(token, 1.0, 1.0, null));
                } catch (IOException ignore) {
                }
            }
        } finally {
            cleanup(req.id());
        }
    }

    private void onNotification(JsonRpcNotification note) throws IOException {
        var handler = notificationHandlers.get(note.method());
        if (handler != null) handler.handle(note);
    }

    private JsonRpcMessage initialize(JsonRpcRequest req) {
        InitializeRequest init = LifecycleCodec.toInitializeRequest(req.params());
        InitializeResponse baseResp;
        try {
            baseResp = lifecycle.initialize(init);
        } catch (UnsupportedProtocolVersionException e) {
            var data = Json.createObjectBuilder()
                    .add("supported", Json.createArrayBuilder().add(e.supported()))
                    .add("requested", e.requested())
                    .build();
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(),
                    "Unsupported protocol version",
                    data));
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
        refreshRootsAsync();
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

    private ProgressToken parseProgressToken(JsonObject params) {
        if (params == null || !params.containsKey("_meta")) return null;
        JsonObject meta = params.getJsonObject("_meta");
        MetaValidator.requireValid(meta);
        if (!meta.containsKey("progressToken")) return null;
        var val = meta.get("progressToken");
        return switch (val.getValueType()) {
            case STRING -> new ProgressToken.StringToken(InputSanitizer.requireClean(meta.getString("progressToken")));
            case NUMBER -> new ProgressToken.NumericToken(meta.getJsonNumber("progressToken").longValue());
            default -> throw new IllegalArgumentException("progressToken must be a string or number");
        };
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
        URI target;
        try {
            target = URI.create(uri);
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!"file".equalsIgnoreCase(target.getScheme())) {
            return true;
        }
        if (roots.isEmpty()) return true;
        for (Root r : roots) {
            try {
                URI base = URI.create(r.uri());
                if ("file".equalsIgnoreCase(base.getScheme())) {
                    String basePath = base.getPath();
                    String targetPath = target.getPath();
                    if (basePath != null && targetPath != null && targetPath.startsWith(basePath)) {
                        return true;
                    }
                }
            } catch (IllegalArgumentException ignore) {
            }
        }
        return false;
    }

    private void cancelled(JsonRpcNotification note) {
        CancelledNotification cn = CancellationCodec.toCancelledNotification(note.params());
        cancellationTracker.cancel(cn.requestId(), cn.reason());
        ProgressToken token = progressTokens.get(cn.requestId());
        if (token != null) {
            progressTracker.release(token);
        }
        try {
            String reason = cancellationTracker.reason(cn.requestId());
            sendLog(LoggingLevel.INFO, "cancellation",
                    reason == null ? JsonValue.NULL : Json.createValue(reason));
        } catch (IOException ignore) {
        }
    }

    private void cleanup(RequestId id) {
        ProgressToken token = progressTokens.remove(id);
        if (token != null) progressTracker.release(token);
        cancellationTracker.release(id);
        idTracker.release(id);
    }

    private void sendProgress(ProgressNotification note) throws IOException {
        if (!progressTracker.isActive(note.token())) return;
        try {
            progressLimiter.requireAllowance(note.token().toString());
            progressTracker.update(note);
        } catch (IllegalArgumentException | IllegalStateException ignore) {
            return;

        }
        send(new JsonRpcNotification(
                "notifications/progress",
                ProgressCodec.toJsonObject(note)));
    }

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        ListResourcesRequest lr = ResourcesCodec.toListResourcesRequest(req.params());
        String cursor = lr.cursor();
        if (cursor != null) {
            try {
                cursor = InputSanitizer.requireClean(cursor);
            } catch (IllegalArgumentException e) {
                return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
            }
        }

        ResourceList list;
        try {
            list = resources.list(cursor);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }

        List<Resource> filteredResources = new java.util.ArrayList<>();
        for (Resource r : list.resources()) {
            if (allowed(r.annotations()) && withinRoots(r.uri())) {
                filteredResources.add(r);
            }
        }

        ListResourcesResult result = new ListResourcesResult(filteredResources, list.nextCursor());
        JsonObject resultJson = ResourcesCodec.toJsonObject(result);
        return new JsonRpcResponse(req.id(), resultJson);
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        ReadResourceRequest rrr;
        try {
            rrr = ResourcesCodec.toReadResourceRequest(req.params());
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        String uri = rrr.uri();
        if (!withinRoots(uri)) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), "Access denied", null));
        }
        ResourceBlock block = resources.read(uri);
        if (block == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    -32002, "Resource not found", Json.createObjectBuilder().add("uri", uri).build()));
        }
        if (!allowed(block.annotations())) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), "Access denied", null));
        }
        ReadResourceResult result = new ReadResourceResult(java.util.List.of(block));
        return new JsonRpcResponse(req.id(), ResourcesCodec.toJsonObject(result));
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        ListResourceTemplatesRequest request;
        try {
            request = ResourcesCodec.toListResourceTemplatesRequest(req.params());
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }

        String cursor = request.cursor();
        if (cursor != null) {
            try {
                cursor = InputSanitizer.requireClean(cursor);
            } catch (IllegalArgumentException e) {
                return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
            }
        }

        ResourceTemplatePage page;
        try {
            page = resources.listTemplates(cursor);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }

        List<ResourceTemplate> filteredTemplates = new java.util.ArrayList<>();
        for (ResourceTemplate t : page.resourceTemplates()) {
            if (allowed(t.annotations())) {
                filteredTemplates.add(t);
            }
        }

        ListResourceTemplatesResult result = new ListResourceTemplatesResult(filteredTemplates, page.nextCursor());
        JsonObject resultJson = ResourcesCodec.toJsonObject(result);
        return new JsonRpcResponse(req.id(), resultJson);
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        SubscribeRequest sr;
        try {
            sr = ResourcesCodec.toSubscribeRequest(req.params());
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        String uri = sr.uri();
        if (!withinRoots(uri)) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), "Access denied", null));
        }
        ResourceBlock existing = resources.read(uri);
        if (existing == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    -32002, "Resource not found", Json.createObjectBuilder().add("uri", uri).build()));
        }
        if (!allowed(existing.annotations())) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), "Access denied", null));
        }
        try {
            ResourceSubscription sub = resources.subscribe(uri, update -> {
                try {
                    ResourceUpdatedNotification n = new ResourceUpdatedNotification(update.uri(), update.title());
                    send(new JsonRpcNotification(
                            "notifications/resources/updated",
                            ResourcesCodec.toJsonObject(n)));
                } catch (IOException ignore) {
                }
            });
            ResourceSubscription prev = resourceSubscriptions.put(uri, sub);
            if (prev != null) {
                try {
                    prev.close();
                } catch (Exception ignore) {
                }
            }
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    private JsonRpcMessage unsubscribeResource(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.RESOURCES);
        UnsubscribeRequest ur;
        try {
            ur = ResourcesCodec.toUnsubscribeRequest(req.params());
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        String uri = ur.uri();
        if (!withinRoots(uri)) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), "Access denied", null));
        }
        ResourceSubscription sub = resourceSubscriptions.remove(uri);
        if (sub != null) {
            try {
                sub.close();
            } catch (Exception ignore) {
            }
        }
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.TOOLS);
        ListToolsRequest ltr = ToolCodec.toListToolsRequest(req.params());
        String cursor = ltr.cursor();
        if (cursor != null) {
            try {
                cursor = InputSanitizer.requireClean(cursor);
            } catch (IllegalArgumentException e) {
                return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
            }
        }
        ToolPage page;
        try {
            page = tools.list(cursor);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        JsonObject result = ToolCodec.toJsonObject(page);
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage callTool(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.TOOLS);
        CallToolRequest callRequest;
        try {
            callRequest = ToolCodec.toCallToolRequest(req.params());
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        try {
            toolLimiter.requireAllowance(callRequest.name());
        } catch (SecurityException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    RATE_LIMIT_CODE, e.getMessage(), null));
        }
        try {
            toolAccess.requireAllowed(principal, callRequest.name());
        } catch (SecurityException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), "Access denied", null));
        }
        try {
            ToolResult result = tools.call(callRequest.name(), callRequest.arguments());
            return new JsonRpcResponse(req.id(), ToolCodec.toJsonObject(result));
        } catch (IllegalArgumentException e) {
            Tool tool = findTool(callRequest.name());
            if (tool != null && lifecycle.negotiatedClientCapabilities().contains(ClientCapability.ELICITATION)) {
                try {
                    ElicitRequest er = new ElicitRequest(
                            "Provide arguments for tool '" + tool.name() + "'",
                            tool.inputSchema(),
                            null);
                    ElicitResult res = elicit(er);
                    if (res.action() == ElicitationAction.ACCEPT) {
                        ToolResult result = tools.call(callRequest.name(), res.content());
                        return new JsonRpcResponse(req.id(), ToolCodec.toJsonObject(result));
                    }
                    return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                            JsonRpcErrorCode.INVALID_PARAMS.code(), "Tool invocation cancelled", null));
                } catch (Exception ex) {
                    return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                            JsonRpcErrorCode.INTERNAL_ERROR.code(), ex.getMessage(), null));
                }
            }
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        ListPromptsRequest lpr = PromptCodec.toListPromptsRequest(req.params());
        String cursor = lpr.cursor();
        if (cursor != null) {
            try {
                cursor = InputSanitizer.requireClean(cursor);
            } catch (IllegalArgumentException e) {
                return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
            }
        }
        PromptPage page;
        try {
            page = prompts.list(cursor);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        JsonObject result = PromptCodec.toJsonObject(page);
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.PROMPTS);
        GetPromptRequest getRequest;
        try {
            getRequest = PromptCodec.toGetPromptRequest(req.params());
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        try {
            PromptInstance inst = prompts.get(getRequest.name(), getRequest.arguments());
            JsonObject result = PromptCodec.toJsonObject(inst);
            return new JsonRpcResponse(req.id(), result);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    private JsonRpcMessage setLogLevel(JsonRpcRequest req) {
        requireServerCapability(ServerCapability.LOGGING);
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            logLevel = LoggingCodec.toSetLevelRequest(params).level();
            return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    private void sendLog(LoggingMessageNotification note) throws IOException {
        logLimiter.requireAllowance(note.logger() == null ? "" : note.logger());
        if (note.level().ordinal() < logLevel.ordinal()) return;
        requireServerCapability(ServerCapability.LOGGING);
        send(new JsonRpcNotification("notifications/message",
                LoggingCodec.toJsonObject(note)));
    }

    private void sendLog(LoggingLevel level, String logger, JsonValue data) throws IOException {
        sendLog(new LoggingMessageNotification(level, logger, data));
    }

    private JsonRpcMessage complete(JsonRpcRequest req) {
        if (!lifecycle.serverCapabilities().contains(ServerCapability.COMPLETIONS)) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                    "Capability not supported",
                    null));
        }
        requireServerCapability(ServerCapability.COMPLETIONS);
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            CompleteRequest request = CompletionCodec.toCompleteRequest(params);
            try {
                completionLimiter.requireAllowance(request.ref().toString());
            } catch (SecurityException e) {
                return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                        RATE_LIMIT_CODE, e.getMessage(), null));
            }
            CompleteResult result = completions.complete(request);
            return new JsonRpcResponse(req.id(), CompletionCodec.toJsonObject(result));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }

    private static final long DEFAULT_TIMEOUT = 30_000L;

    private JsonRpcMessage sendRequest(String method, JsonObject params) throws IOException {
        return sendRequest(method, params, DEFAULT_TIMEOUT);
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
                        "notifications/cancelled",
                        CancellationCodec.toJsonObject(new CancelledNotification(id, "timeout"))));
            } catch (IOException ignore) {
            }
            throw new IOException("Request timed out after " + timeoutMillis + " ms");
        } finally {
            pending.remove(id);
        }
    }

    public List<Root> listRoots() throws IOException {
        List<Root> fetched = fetchRoots();
        roots.clear();
        roots.addAll(fetched);
        return List.copyOf(fetched);
    }

    private List<Root> fetchRoots() throws IOException {
        requireClientCapability(ClientCapability.ROOTS);
        JsonRpcMessage msg = sendRequest("roots/list", RootsCodec.toJsonObject(new ListRootsRequest()));
        if (msg instanceof JsonRpcResponse resp) {
            return RootsCodec.toRoots(resp.result());
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    public RootsSubscription subscribeRoots(RootsListener listener) {
        rootsListeners.add(listener);
        return () -> rootsListeners.remove(listener);
    }

    public List<Root> roots() {
        return List.copyOf(roots);
    }

    private void refreshRootsAsync() {
        if (!lifecycle.negotiatedClientCapabilities().contains(ClientCapability.ROOTS)) return;
        Thread t = new Thread(() -> {
            try {
                List<Root> updated = fetchRoots();
                roots.clear();
                roots.addAll(updated);
                rootsListeners.forEach(RootsListener::listChanged);
            } catch (IOException ignore) {
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void rootsListChanged() {
        refreshRootsAsync();
    }

    public ElicitResult elicit(ElicitRequest req) throws IOException {
        requireClientCapability(ClientCapability.ELICITATION);
        JsonRpcMessage msg = sendRequest("elicitation/create", ElicitCodec.toJsonObject(req));
        if (msg instanceof JsonRpcResponse resp) {
            ElicitResult er = ElicitCodec.toResult(resp.result());
            if (er.action() == ElicitationAction.ACCEPT) {
                SchemaValidator.validate(req.requestedSchema(), er.content());
            }
            return er;
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private Tool findTool(String name) {
        if (tools == null) return null;
        ToolPage page = tools.list(null);
        for (Tool t : page.tools()) {
            if (t.name().equals(name)) return t;
        }
        return null;
    }

    public CreateMessageResponse createMessage(CreateMessageRequest req) throws IOException {
        requireClientCapability(ClientCapability.SAMPLING);
        samplingAccess.requireAllowed(principal);
        JsonRpcMessage msg = sendRequest("sampling/createMessage", SamplingCodec.toJsonObject(req));
        if (msg instanceof JsonRpcResponse resp) {
            return SamplingCodec.toCreateMessageResponse(resp.result());
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private JsonRpcMessage handleCreateMessage(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            CreateMessageRequest cmr = SamplingCodec.toCreateMessageRequest(params);
            CreateMessageResponse resp = createMessage(cmr);
            return new JsonRpcResponse(req.id(), SamplingCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }

    private static ResourceProvider createDefaultResources() {
        Resource r = new Resource("test://example", "example", null, null, "text/plain", 5L, null, null);
        ResourceBlock.Text block = new ResourceBlock.Text("test://example", "text/plain", "hello", null, null);
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
        PromptMessageTemplate msg = new PromptMessageTemplate(Role.USER, new PromptContent.Text("hello", null, null));
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
            try {
                sub.close();
            } catch (Exception ignore) {
            }
        }
        resourceSubscriptions.clear();
        if (resourceListSubscription != null) {
            try {
                resourceListSubscription.close();
            } catch (Exception ignore) {
            }
            resourceListSubscription = null;
        }
        if (toolListSubscription != null) {
            try {
                toolListSubscription.close();
            } catch (Exception ignore) {
            }
            toolListSubscription = null;
        }
        if (promptsSubscription != null) {
            try {
                promptsSubscription.close();
            } catch (Exception ignore) {
            }
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
