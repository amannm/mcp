package com.amannmalik.mcp.jsonrpc;

import jakarta.json.JsonObject;


public record JsonRpcRequest(RequestId id, String method, JsonObject params) implements JsonRpcMessage {
    public JsonRpcRequest {
        if (id == null || method == null) {
            throw new IllegalArgumentException("id and method are required");
        }
    }

    @Override
    public String jsonrpc() {
        return JsonRpc.VERSION;
    }
}
