package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.jsonrpc.*;
import com.amannmalik.mcp.lifecycle.ServerCapability;
import com.amannmalik.mcp.server.McpServer;
import com.amannmalik.mcp.transport.Transport;
import jakarta.json.JsonObject;

import java.util.EnumSet;

/** McpServer extension providing sampling support. */
public class SamplingServer extends McpServer {
    private final SamplingProvider provider;

    public SamplingServer(SamplingProvider provider, Transport transport) {
        super(EnumSet.noneOf(ServerCapability.class), transport);
        this.provider = provider;
        registerRequestHandler("sampling/createMessage", this::createMessage);
    }

    private JsonRpcMessage createMessage(JsonRpcRequest req) {
        JsonObject params = req.params();
        if (params == null) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), "Missing params", null));
        }
        try {
            CreateMessageRequest cmr = SamplingCodec.toCreateMessageRequest(params);
            CreateMessageResponse resp = provider.createMessage(cmr);
            return new JsonRpcResponse(req.id(), SamplingCodec.toJsonObject(resp));
        } catch (IllegalArgumentException e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INVALID_PARAMS.code(), e.getMessage(), null));
        } catch (Exception e) {
            return new JsonRpcError(req.id(), new JsonRpcError.ErrorDetail(
                    JsonRpcErrorCode.INTERNAL_ERROR.code(), e.getMessage(), null));
        }
    }
}
