package com.amannmalik.mcp.server;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.EnumSet;

/** Minimal server supporting ping and basic list operations. */
public final class BasicServer extends McpServer {
    public BasicServer(Transport transport) {
        super(EnumSet.of(ServerCapability.RESOURCES, ServerCapability.TOOLS, ServerCapability.PROMPTS), transport);
        registerRequestHandler("resources/list", this::listResources);
        registerRequestHandler("tools/list", this::listTools);
        registerRequestHandler("prompts/list", this::listPrompts);
    }

    private JsonRpcMessage listResources(JsonRpcRequest req) {
        JsonObject result = Json.createObjectBuilder()
                .add("resources", Json.createArrayBuilder().build())
                .build();
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        JsonObject result = Json.createObjectBuilder()
                .add("tools", Json.createArrayBuilder().build())
                .build();
        return new JsonRpcResponse(req.id(), result);
    }

    private JsonRpcMessage listPrompts(JsonRpcRequest req) {
        JsonObject result = Json.createObjectBuilder()
                .add("prompts", Json.createArrayBuilder().build())
                .build();
        return new JsonRpcResponse(req.id(), result);
    }
}
