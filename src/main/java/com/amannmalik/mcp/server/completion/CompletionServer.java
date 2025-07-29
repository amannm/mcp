package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;
import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;

import java.util.EnumSet;

/** McpServer extension providing completion support. */
public class CompletionServer extends McpServer {
    private final CompletionProvider provider;

    public CompletionServer(CompletionProvider provider, Transport transport) {
        super(EnumSet.of(ServerCapability.COMPLETIONS), transport);
        this.provider = provider;
    }

    public static CompletionServer create(CompletionProvider provider, Transport transport) {
        CompletionServer server = new CompletionServer(provider, transport);
        server.registerRequestHandler("completion/complete", server::complete);
        return server;
    }

    private JsonRpcMessage complete(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            CompleteRequest request = CompletionCodec.toCompleteRequest(params);
            CompleteResult result = provider.complete(request);
            return new JsonRpcResponse(req.id(), CompletionCodec.toJsonObject(result));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }
}
