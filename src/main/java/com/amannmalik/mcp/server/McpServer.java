package com.amannmalik.mcp.server;

import com.amannmalik.mcp.auth.Principal;
import com.amannmalik.mcp.client.elicitation.ElicitationCodec;
import com.amannmalik.mcp.client.elicitation.ElicitationRequest;
import com.amannmalik.mcp.client.elicitation.ElicitationResponse;
import com.amannmalik.mcp.client.roots.ListRootsRequest;
import com.amannmalik.mcp.client.roots.Root;
import com.amannmalik.mcp.client.roots.RootsCodec;
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
import com.amannmalik.mcp.ping.PingCodec;
import com.amannmalik.mcp.ping.PingRequest;
import com.amannmalik.mcp.prompts.InMemoryPromptProvider;
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
import com.amannmalik.mcp.server.completion.CompleteRequest;
import com.amannmalik.mcp.server.completion.CompleteResult;
import com.amannmalik.mcp.server.completion.CompletionCodec;
import com.amannmalik.mcp.server.completion.CompletionProvider;
import com.amannmalik.mcp.server.completion.InMemoryCompletionProvider;
import com.amannmalik.mcp.server.logging.LoggingCodec;
import com.amannmalik.mcp.server.logging.LoggingLevel;
import com.amannmalik.mcp.server.logging.LoggingNotification;
import com.amannmalik.mcp.server.resources.Audience;
import com.amannmalik.mcp.server.resources.InMemoryResourceProvider;
import com.amannmalik.mcp.server.resources.Resource;
import com.amannmalik.mcp.server.resources.ResourceAnnotations;
import com.amannmalik.mcp.server.resources.ResourceBlock;
import com.amannmalik.mcp.server.resources.ResourceList;
import com.amannmalik.mcp.server.resources.ResourceListSubscription;
import com.amannmalik.mcp.server.resources.ResourceProvider;
import com.amannmalik.mcp.server.resources.ResourceSubscription;
import com.amannmalik.mcp.server.resources.ResourceTemplate;
import com.amannmalik.mcp.server.resources.ResourceTemplatePage;
import com.amannmalik.mcp.server.resources.ResourceUpdatedNotification;
import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.server.tools.InMemoryToolProvider;
import com.amannmalik.mcp.server.tools.Tool;
import com.amannmalik.mcp.server.tools.ToolCodec;
import com.amannmalik.mcp.server.tools.ToolListSubscription;
import com.amannmalik.mcp.server.tools.ToolPage;
import com.amannmalik.mcp.server.tools.ToolProvider;
import com.amannmalik.mcp.server.tools.ToolResult;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.util.CancellationCodec;
import com.amannmalik.mcp.util.CancellationTracker;
import com.amannmalik.mcp.util.CancelledNotification;
import com.amannmalik.mcp.util.PaginatedResult;
import com.amannmalik.mcp.util.PaginationCodec;
import com.amannmalik.mcp.util.ProgressCodec;
import com.amannmalik.mcp.util.ProgressNotification;
import com.amannmalik.mcp.util.ProgressToken;
import com.amannmalik.mcp.util.ProgressTracker;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.io.EOFException;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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
    private final ResourceAccessController resourceAccess;
    private final Principal principal;
    private volatile LoggingLevel logLevel = LoggingLevel.INFO;
    private static final int RATE_LIMIT_CODE = -32001;
    private final RateLimiter toolLimiter = new RateLimiter(5, 1000);
    private final RateLimiter completionLimiter = new RateLimiter(10, 1000);
    private final RateLimiter logLimiter = new RateLimiter(20, 1000);
    private final RateLimiter progressLimiter = new RateLimiter(20, 1000);
    private final AtomicLong requestCounter = new AtomicLong(1);
    private final Map<RequestId, CompletableFuture<JsonRpcMessage>> pending = new ConcurrentHashMap<>();

    public McpServer(Transport transport) {
        this(createDefaultResources(), createDefaultTools(), createDefaultPrompts(), createDefaultCompletions(),
                createDefaultPrivacyBoundary("default"),
                new Principal("default", java.util.Set.of()),
                transport);
    }

    McpServer(ResourceProvider resources,
              ToolProvider tools,
              PromptProvider prompts,
              CompletionProvider completions,
              Transport transport) {
        this(resources, tools, prompts, completions,
                createDefaultPrivacyBoundary("default"),
                new Principal("default", java.util.Set.of()),
                transport);
    }

    McpServer(ResourceProvider resources,
              ToolProvider tools,
              PromptProvider prompts,
              CompletionProvider completions,
              ResourceAccessController resourceAccess,
              Principal principal,
              Transport transport) {
        this.transport = transport;
        this.lifecycle = new ProtocolLifecycle(EnumSet.of(
                ServerCapability.RESOURCES,
                ServerCapability.TOOLS,
                ServerCapability.PROMPTS,
                ServerCapability.LOGGING,
                ServerCapability.COMPLETIONS));
        this.resources = resources;
        this.tools = tools;
        this.prompts = prompts;
        this.completions = completions;
        this.resourceAccess = resourceAccess;
        this.principal = principal;

        try {
            resourceListSubscription = resources.subscribeList(() -> {
                try {
                    send(new JsonRpcNotification("notifications/resources/list_changed", null));
                } catch (IOException ignore) {
                }
            });
        } catch (Exception ignore) {
        }

        try {
            toolListSubscription = tools.subscribeList(() -> {
                try {
                    send(new JsonRpcNotification("notifications/tools/list_changed", null));
                } catch (IOException ignore) {
                }
            });
        } catch (Exception ignore) {
        }

        try {
            promptsSubscription = prompts.subscribe(() -> {
                try {
                    send(new JsonRpcNotification("notifications/prompts/list_changed", null));
                } catch (IOException ignore) {
                }
            });
        } catch (Exception ignore) {
        }

        registerRequestHandler("initialize", this::initialize);
        registerNotificationHandler("notifications/initialized", this::initialized);
        registerRequestHandler("ping", this::ping);
        registerNotificationHandler("notifications/cancelled", this::cancelled);

        registerRequestHandler("resources/list", this::listResources);
        registerRequestHandler("resources/read", this::readResource);
        registerRequestHandler("resources/templates/list", this::listTemplates);
        registerRequestHandler("resources/subscribe", this::subscribeResource);
        registerRequestHandler("resources/unsubscribe", this::unsubscribeResource);

        registerRequestHandler("tools/list", this::listTools);
        registerRequestHandler("tools/call", this::callTool);

        registerRequestHandler("prompts/list", this::listPrompts);
        registerRequestHandler("prompts/get", this::getPrompt);

        registerRequestHandler("logging/setLevel", this::setLogLevel);

        registerRequestHandler("completion/complete", this::complete);
    }

    private ProtocolLifecycle lifecycle() {
        return lifecycle;
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
            } catch (jakarta.json.stream.JsonParsingException e) {
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
        var handler = requestHandlers.get(req.method());
        if (handler == null) {
            send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.METHOD_NOT_FOUND.code(),
                    "Unknown method: " + req.method(), null)));
            return;
        }
        try {
            idTracker.register(req.id());
        } catch (IllegalArgumentException e) {
            send(new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_REQUEST.code(), e.getMessage(), null)));
            return;
        }
        ProgressToken token;
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
            cleanup(req.id());
            return;
        }
        boolean cancellable = !"initialize".equals(req.method());
        if (cancellable) {
            cancellationTracker.register(req.id());
        }
        JsonRpcMessage resp = handler.handle(req);
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
        cleanup(req.id());
    }

    private void onNotification(JsonRpcNotification note) throws IOException {
        var handler = notificationHandlers.get(note.method());
        if (handler != null) handler.handle(note);
    }

    private JsonRpcMessage initialize(JsonRpcRequest req) {
        InitializeRequest init = LifecycleCodec.toInitializeRequest(req.params());
        InitializeResponse resp = lifecycle.initialize(init);
        return new JsonRpcResponse(req.id(), LifecycleCodec.toJsonObject(resp));
    }

    private void initialized(JsonRpcNotification ignored) {
        lifecycle.initialized();
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
        com.amannmalik.mcp.validation.MetaValidator.requireValid(meta);
        if (!meta.containsKey("progressToken")) return null;
        var val = meta.get("progressToken");
        return switch (val.getValueType()) {
            case STRING -> new ProgressToken.StringToken(meta.getString("progressToken"));
            case NUMBER -> new ProgressToken.NumericToken(meta.getJsonNumber("progressToken").longValue());
            default -> null;
        };
    }

    private boolean allowed(ResourceAnnotations ann) {
        try {
            resourceAccess.requireAllowed(principal, ann);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    private void cancelled(JsonRpcNotification note) {
        CancelledNotification cn = CancellationCodec.toCancelledNotification(note.params());
        cancellationTracker.cancel(cn.requestId(), cn.reason());
        ProgressToken token = progressTokens.get(cn.requestId());
        if (token != null) {
            progressTracker.release(token);
        }
    }

    private void cleanup(RequestId id) {
        ProgressToken token = progressTokens.remove(id);
        if (token != null) progressTracker.release(token);
        cancellationTracker.release(id);
        idTracker.release(id);
    }

    private void sendProgress(ProgressNotification note) throws IOException {
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
        String cursor = PaginationCodec.toPaginatedRequest(req.params()).cursor();
        ResourceList list;
        try {
            list = resources.list(cursor);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        var arr = Json.createArrayBuilder();
        for (Resource r : list.resources()) {
            if (allowed(r.annotations())) arr.add(ResourcesCodec.toJsonObject(r));
        }
        var b = Json.createObjectBuilder().add("resources", arr.build());
        PaginationCodec.toJsonObject(new PaginatedResult(list.nextCursor())).forEach(b::add);
        return new JsonRpcResponse(req.id(), b.build());
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null || !params.containsKey("uri")) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "uri required", null));
        }
        String uri = params.getString("uri");
        try {
            uri = com.amannmalik.mcp.validation.UriValidator.requireAbsolute(uri);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
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
        JsonObject result = Json.createObjectBuilder()
                .add("contents", Json.createArrayBuilder().add(ResourcesCodec.toJsonObject(block)).build())
                .build();
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        String cursor = PaginationCodec.toPaginatedRequest(req.params()).cursor();
        ResourceTemplatePage page;
        try {
            page = resources.listTemplates(cursor);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        var arr = Json.createArrayBuilder();
        page.resourceTemplates().forEach(t -> {
            if (allowed(t.annotations())) arr.add(ResourcesCodec.toJsonObject(t));
        });
        var b = Json.createObjectBuilder().add("resourceTemplates", arr.build());
        PaginationCodec.toJsonObject(new PaginatedResult(page.nextCursor())).forEach(b::add);
        return new JsonRpcResponse(req.id(), b.build());
    }

    private JsonRpcMessage subscribeResource(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null || !params.containsKey("uri")) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "uri required", null));
        }
        String uri = params.getString("uri");
        try {
            uri = com.amannmalik.mcp.validation.UriValidator.requireAbsolute(uri);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        try {
            ResourceSubscription sub = resources.subscribe(uri, update -> {
                try {
                    ResourceUpdatedNotification n = new ResourceUpdatedNotification(
                            update.uri(), update.title());
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
        JsonObject params = req.params();
        if (params == null || !params.containsKey("uri")) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "uri required", null));
        }
        String uri = params.getString("uri");
        try {
            uri = com.amannmalik.mcp.validation.UriValidator.requireAbsolute(uri);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
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
        String cursor = PaginationCodec.toPaginatedRequest(req.params()).cursor();
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
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        String name = params.getString("name", null);
        JsonObject args = params.getJsonObject("arguments");
        if (name == null || args == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing name or arguments", null));
        }
        try {
            toolLimiter.requireAllowance(name);
        } catch (SecurityException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    RATE_LIMIT_CODE, e.getMessage(), null));
        }
        try {
            ToolResult result = tools.call(name, args);
            return new JsonRpcResponse(req.id(), ToolCodec.toJsonObject(result));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        String cursor = PaginationCodec.toPaginatedRequest(req.params()).cursor();
        PromptPage page;
        try {
            page = prompts.list(cursor);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        var arr = Json.createArrayBuilder();
        for (Prompt p : page.prompts()) arr.add(PromptCodec.toJsonObject(p));
        var builder = Json.createObjectBuilder().add("prompts", arr.build());
        PaginationCodec.toJsonObject(new PaginatedResult(page.nextCursor())).forEach(builder::add);
        return new JsonRpcResponse(req.id(), builder.build());
    }

    private JsonRpcMessage getPrompt(JsonRpcRequest req) {
        JsonObject params = req.params();
        String name = params.getString("name", null);
        if (name == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(),
                    "name is required", null));
        }
        Map<String, String> args = PromptCodec.toArguments(params.getJsonObject("arguments"));
        try {
            PromptInstance inst = prompts.get(name, args);
            JsonObject result = PromptCodec.toJsonObject(inst);
            return new JsonRpcResponse(req.id(), result);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    private JsonRpcMessage setLogLevel(JsonRpcRequest req) {
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

    private void sendLog(LoggingNotification note) throws IOException {
        logLimiter.requireAllowance(note.logger() == null ? "" : note.logger());
        if (note.level().ordinal() < logLevel.ordinal()) return;
        requireServerCapability(ServerCapability.LOGGING);
        send(new JsonRpcNotification("notifications/message",
                LoggingCodec.toJsonObject(note)));
    }

    private void sendLog(LoggingLevel level, String logger, jakarta.json.JsonValue data) throws IOException {
        sendLog(new LoggingNotification(level, logger, data));
    }

    private JsonRpcMessage complete(JsonRpcRequest req) {
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

    private JsonRpcMessage sendRequest(String method, JsonObject params) throws IOException {
        RequestId id = new RequestId.NumericId(requestCounter.getAndIncrement());
        CompletableFuture<JsonRpcMessage> future = new CompletableFuture<>();
        pending.put(id, future);
        send(new JsonRpcRequest(id, method, params));
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException io) throw io;
            throw new IOException(cause);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IOException("Request timed out after 30 seconds");
        } finally {
            pending.remove(id);
        }
    }

    public List<Root> listRoots() throws IOException {
        requireClientCapability(ClientCapability.ROOTS);
        JsonRpcMessage msg = sendRequest("roots/list", RootsCodec.toJsonObject(new ListRootsRequest()));
        if (msg instanceof JsonRpcResponse resp) {
            return RootsCodec.toRoots(resp.result());
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    public ElicitationResponse elicit(ElicitationRequest req) throws IOException {
        requireClientCapability(ClientCapability.ELICITATION);
        JsonRpcMessage msg = sendRequest("elicitation/create", ElicitationCodec.toJsonObject(req));
        if (msg instanceof JsonRpcResponse resp) {
            return ElicitationCodec.toResponse(resp.result());
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    public CreateMessageResponse createMessage(CreateMessageRequest req) throws IOException {
        requireClientCapability(ClientCapability.SAMPLING);
        JsonRpcMessage msg = sendRequest("sampling/createMessage", SamplingCodec.toJsonObject(req));
        if (msg instanceof JsonRpcResponse resp) {
            return SamplingCodec.toCreateMessageResponse(resp.result());
        }
        throw new IOException(((JsonRpcError) msg).error().message());
    }

    private static ResourceProvider createDefaultResources() {
        Resource r = new Resource("test://example", "example", null, null, "text/plain", 5L, null, null);
        ResourceBlock.Text block = new ResourceBlock.Text("test://example", "example", null, "text/plain", "hello", null, null);
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
                                .build(), null, false)));
    }

    private static PromptProvider createDefaultPrompts() {
        InMemoryPromptProvider p = new InMemoryPromptProvider();
        PromptArgument arg = new PromptArgument("test_arg", null, null, true);
        Prompt prompt = new Prompt("test_prompt", "Test Prompt", null, List.of(arg));
        PromptMessageTemplate msg = new PromptMessageTemplate(Role.USER, new PromptContent.Text("hello", null));
        p.add(new PromptTemplate(prompt, List.of(msg)));
        return p;
    }

    private static CompletionProvider createDefaultCompletions() {
        InMemoryCompletionProvider provider = new InMemoryCompletionProvider();
        provider.add(new CompleteRequest.Ref.PromptRef("test_prompt"), "test_arg", Map.of(), List.of("test_completion"));
        return provider;
    }

    private static ResourceAccessController createDefaultPrivacyBoundary(String principalId) {
        var p = new PrivacyBoundaryEnforcer();
        for (Audience a : Audience.values()) p.allow(principalId, a);
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
        resources.close();
        completions.close();
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
