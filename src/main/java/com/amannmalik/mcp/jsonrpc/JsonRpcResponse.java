package com.amannmalik.mcp.jsonrpc;

import jakarta.json.JsonObject;

/** A successful JSON-RPC response. */
public record JsonRpcResponse(RequestId id, JsonObject result) implements JsonRpcMessage {
    public JsonRpcResponse {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }

    @Override
    public String jsonrpc() {
        return JsonRpc.VERSION;
    }
}
