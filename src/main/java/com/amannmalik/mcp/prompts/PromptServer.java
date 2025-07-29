package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** McpServer extension that exposes prompts/list and prompts/get. */
public class PromptServer extends McpServer {
    private final PromptProvider provider;

    public PromptServer(PromptProvider provider, Transport transport) {
        super(EnumSet.of(ServerCapability.PROMPTS), transport);
        this.provider = provider;
    }

    public static PromptServer create(PromptProvider provider, Transport transport) {
        PromptServer server = new PromptServer(provider, transport);
        server.registerRequestHandler("prompts/list", server::listPrompts);
        server.registerRequestHandler("prompts/get", server::getPrompt);
        return server;
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        String cursor = req.params() == null ? null : req.params().getString("cursor", null);
        PromptPage page;
        try {
            page = provider.list(cursor);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
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
            PromptInstance inst = provider.get(name, args);
            JsonObject result = PromptCodec.toJsonObject(inst);
            return new JsonRpcResponse(req.id(), result);
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    /** Notify clients that the list of prompts has changed. */
    public void listChanged() throws IOException {
        send(new JsonRpcNotification("notifications/prompts/list_changed", null));
    }
}
