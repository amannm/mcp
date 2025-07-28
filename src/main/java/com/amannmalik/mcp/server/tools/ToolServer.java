package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.Transport;
import com.amannmalik.mcp.security.RateLimiter;
import jakarta.json.JsonObject;
import java.io.IOException;

import java.util.EnumSet;

/** McpServer extension providing tool listing and invocation support. */
public class ToolServer extends McpServer {
    private final ToolProvider provider;
    private final RateLimiter limiter;

    private ToolServer(ToolProvider provider, Transport transport, RateLimiter limiter) {
        super(EnumSet.of(ServerCapability.TOOLS), transport);
        this.provider = provider;
        this.limiter = limiter;
    }

    public static ToolServer create(ToolProvider provider, Transport transport) {
        return create(provider, transport, new RateLimiter(Integer.MAX_VALUE, 1));
    }

    public static ToolServer create(ToolProvider provider, Transport transport, RateLimiter limiter) {
        ToolServer server = new ToolServer(provider, transport, limiter);
        server.registerRequestHandler("tools/list", server::listTools);
        server.registerRequestHandler("tools/call", server::callTool);
        return server;
    }

    private JsonRpcMessage listTools(JsonRpcRequest req) {
        String cursor = req.params() == null ? null : req.params().getString("cursor", null);
        ToolPage page = provider.list(cursor);
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
            limiter.requireAllowance(name);
        } catch (SecurityException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
        try {
            ToolResult result = provider.call(name, args);
            return new JsonRpcResponse(req.id(), ToolCodec.toJsonObject(result));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        }
    }

    /** Notify clients that the list of tools has changed. */
    public void listChanged() throws IOException {
        send(new JsonRpcNotification("notifications/tools/list_changed", null));
    }
}
