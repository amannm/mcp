package com.amannmalik.mcp.api;

import com.amannmalik.mcp.api.config.McpServerConfiguration;
import com.amannmalik.mcp.codec.*;
import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.spi.*;
import com.amannmalik.mcp.util.RateLimiter;
import jakarta.json.JsonObject;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/// - [Tools](specification/2025-06-18/server/tools.mdx)
final class ToolCallHandler {
    private static final CallToolRequestAbstractEntityCodec REQUEST_CODEC = new CallToolRequestAbstractEntityCodec();
    private static final JsonCodec<ToolResult> RESULT_CODEC = new ToolResultAbstractEntityCodec();

    private final ToolProvider tools;
    private final ToolAccessPolicy access;
    private final RateLimiter limiter;
    private final Principal principal;
    private final McpServerConfiguration config;
    private final Supplier<Set<ClientCapability>> clientCaps;
    private final Elicitor elicitor;

    ToolCallHandler(
            ToolProvider tools,
            ToolAccessPolicy access,
            RateLimiter limiter,
            Principal principal,
            McpServerConfiguration config,
            Supplier<Set<ClientCapability>> clientCaps,
            Elicitor elicitor) {
        this.tools = tools;
        this.access = access;
        this.limiter = limiter;
        this.principal = principal;
        this.config = config;
        this.clientCaps = clientCaps;
        this.elicitor = elicitor;
    }

    JsonRpcMessage handle(JsonRpcRequest req) {
        CallToolRequest callRequest;
        try {
            callRequest = REQUEST_CODEC.fromJson(req.params());
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        }
        var limit = rateLimit(callRequest.name());
        if (limit.isPresent()) {
            return JsonRpcError.of(req.id(), config.rateLimitErrorCode(), limit.get());
        }
        var tool = tools.find(callRequest.name()).orElse(null);
        if (tool == null) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Unknown tool: " + callRequest.name());
        }
        try {
            access.requireAllowed(principal, tool);
        } catch (SecurityException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, config.errorAccessDenied());
        }
        return invoke(req, tool, callRequest.arguments());
    }

    private JsonRpcMessage invoke(JsonRpcRequest req, Tool tool, JsonObject args) {
        try {
            var result = tools.call(tool.name(), args);
            return new JsonRpcResponse(req.id(), RESULT_CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return recover(req, tool, e);
        }
    }

    private JsonRpcMessage recover(JsonRpcRequest req, Tool tool, IllegalArgumentException failure) {
        if (!clientCaps.get().contains(ClientCapability.ELICITATION)) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, failure.getMessage());
        }
        try {
            var er = new ElicitRequest(
                    "Provide arguments for tool '" + tool.name() + "'",
                    tool.inputSchema(),
                    null);
            var res = elicitor.elicit(er);
            if (res.action() != ElicitationAction.ACCEPT) {
                return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, "Tool invocation cancelled");
            }
            var result = tools.call(tool.name(), res.content());
            return new JsonRpcResponse(req.id(), RESULT_CODEC.toJson(result));
        } catch (IllegalArgumentException e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INVALID_PARAMS, e.getMessage());
        } catch (Exception e) {
            return JsonRpcError.of(req.id(), JsonRpcErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    private Optional<String> rateLimit(String key) {
        try {
            limiter.requireAllowance(key);
            return Optional.empty();
        } catch (SecurityException e) {
            return Optional.of(e.getMessage());
        }
    }

    @FunctionalInterface
    interface Elicitor {
        ElicitResult elicit(ElicitRequest request) throws Exception;
    }
}

