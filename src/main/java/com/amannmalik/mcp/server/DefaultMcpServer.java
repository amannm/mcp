package com.amannmalik.mcp.server;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.prompts.*;
import com.amannmalik.mcp.server.completion.*;
import com.amannmalik.mcp.server.logging.LoggingLevel;
import com.amannmalik.mcp.server.resources.*;
import com.amannmalik.mcp.server.tools.*;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Reference server wiring together all protocol primitives. */
public final class DefaultMcpServer extends McpServer {
    private final ResourceProvider resources;
    private final ToolProvider tools;
    private final PromptProvider prompts;
    private final CompletionProvider completions;
    private volatile LoggingLevel logLevel = LoggingLevel.INFO;

    public DefaultMcpServer(Transport transport) {
        this(createDefaultResources(), createDefaultTools(), createDefaultPrompts(), createDefaultCompletions(), transport);
    }

    DefaultMcpServer(ResourceProvider resources,
                     ToolProvider tools,
                     PromptProvider prompts,
                     CompletionProvider completions,
                     Transport transport) {
        super(EnumSet.of(ServerCapability.RESOURCES,
                ServerCapability.TOOLS,
                ServerCapability.PROMPTS,
                ServerCapability.LOGGING,
                ServerCapability.COMPLETIONS), transport);
        this.resources = resources;
        this.tools = tools;
        this.prompts = prompts;
        this.completions = completions;

        // Resource handlers
        registerRequestHandler("resources/list", this::listResources);
        registerRequestHandler("resources/read", this::readResource);
        registerRequestHandler("resources/templates/list", this::listTemplates);

        // Tool handlers
        registerRequestHandler("tools/list", this::listTools);
        registerRequestHandler("tools/call", this::callTool);

        // Prompt handlers
        registerRequestHandler("prompts/list", this::listPrompts);
        registerRequestHandler("prompts/get", this::getPrompt);

        // Logging
        registerRequestHandler("logging/setLevel", this::setLogLevel);

        // Completion
        registerRequestHandler("completion/complete", this::complete);
    }

    // ----- Resource operations -----

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        String cursor = req.params() == null ? null : req.params().getString("cursor", null);
        ResourceList list;
        try {
            list = resources.list(cursor);
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
        JsonArrayBuilder arr = Json.createArrayBuilder();
        for (Resource r : list.resources()) {
            arr.add(ResourcesCodec.toJsonObject(r));
        }
        JsonObjectBuilder b = Json.createObjectBuilder().add("resources", arr.build());
        if (list.nextCursor() != null) b.add("nextCursor", list.nextCursor());
        return new JsonRpcResponse(req.id(), b.build());
    }

    private JsonRpcMessage readResource(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null || !params.containsKey("uri")) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "uri required", null));
        }
        String uri = params.getString("uri");
        ResourceBlock block;
        try {
            block = resources.read(uri);
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
        if (block == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    -32002, "Resource not found", Json.createObjectBuilder().add("uri", uri).build()));
        }
        JsonObject result = Json.createObjectBuilder()
                .add("contents", Json.createArrayBuilder().add(ResourcesCodec.toJsonObject(block)).build())
                .build();
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage listTemplates(JsonRpcRequest req) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        try {
            for (ResourceTemplate t : resources.templates()) {
                arr.add(ResourcesCodec.toJsonObject(t));
            }
        } catch (IOException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
        JsonObject result = Json.createObjectBuilder().add("resourceTemplates", arr.build()).build();
        return new JsonRpcResponse(req.id(), result);
    }

    // ----- Tool operations -----

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        String cursor = req.params() == null ? null : req.params().getString("cursor", null);
        ToolPage page = tools.list(cursor);
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
            ToolResult result = tools.call(name, args);
            return new JsonRpcResponse(req.id(), ToolCodec.toJsonObject(result));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    // ----- Prompt operations -----

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        String cursor = req.params() == null ? null : req.params().getString("cursor", null);
        PromptPage page = prompts.list(cursor);
        var arr = Json.createArrayBuilder();
        for (Prompt p : page.prompts()) arr.add(PromptCodec.toJsonObject(p));
        JsonObjectBuilder builder = Json.createObjectBuilder().add("prompts", arr.build());
        if (page.nextCursor() != null) builder.add("nextCursor", page.nextCursor());
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

    // ----- Logging -----

    private JsonRpcMessage setLogLevel(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        logLevel = LoggingLevel.valueOf(params.getString("level").toUpperCase());
        return new JsonRpcResponse(req.id(), Json.createObjectBuilder().build());
    }

    // ----- Completion -----

    private JsonRpcMessage complete(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            CompleteRequest request = CompletionCodec.toCompleteRequest(params);
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

    // ----- Default data -----

    private static ResourceProvider createDefaultResources() {
        Resource r = new Resource("test://example", "example", null, null, "text/plain", 5L, null);
        ResourceBlock.Text block = new ResourceBlock.Text("test://example", "example", null, "text/plain", "hello", null);
        ResourceTemplate t = new ResourceTemplate("test://template", "example_template", null, null, "text/plain", null);
        return new InMemoryResourceProvider(List.of(r), Map.of(r.uri(), block), List.of(t));
    }

    private static ToolProvider createDefaultTools() {
        var schema = Json.createObjectBuilder().add("type", "object").build();
        Tool tool = new Tool("test_tool", "Test Tool", null, schema, null, null);
        InMemoryToolProvider provider = new InMemoryToolProvider(
                List.of(tool),
                Map.of("test_tool", a -> new ToolResult(
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("type", "text")
                                        .add("text", "ok")
                                        .build())
                                .build(), null, false)));
        return provider;
    }

    private static PromptProvider createDefaultPrompts() {
        InMemoryPromptProvider p = new InMemoryPromptProvider();
        PromptArgument arg = new PromptArgument("test_arg", null, null, false);
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
}
