package com.amannmalik.mcp.client.elicitation;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;

import java.util.EnumSet;

/** McpServer extension providing elicitation support. */
public class ElicitationServer extends McpServer {
    private final ElicitationProvider provider;

    public ElicitationServer(ElicitationProvider provider, Transport transport) {
        super(EnumSet.noneOf(ServerCapability.class), transport);
        this.provider = provider;
    }

    public static ElicitationServer create(ElicitationProvider provider, Transport transport) {
        ElicitationServer server = new ElicitationServer(provider, transport);
        server.registerRequestHandler("elicitation/create", server::create);
        return server;
    }

    private JsonRpcMessage create(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            ElicitationRequest er = ElicitationCodec.toRequest(params);
            ElicitationResponse resp = provider.elicit(er);
            return new JsonRpcResponse(req.id(), ElicitationCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }
}
