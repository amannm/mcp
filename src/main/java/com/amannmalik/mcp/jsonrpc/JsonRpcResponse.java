package com.amannmalik.mcp.jsonrpc;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.RequestId;
import jakarta.json.JsonObject;

public record JsonRpcResponse(RequestId id, JsonObject result) implements JsonRpcMessage {
    public JsonRpcResponse {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (result == null) {
            throw new IllegalArgumentException("result is required");
        }
    }

    @Override
    public String jsonrpc() {
        return JsonRpc.VERSION;
    }
}
