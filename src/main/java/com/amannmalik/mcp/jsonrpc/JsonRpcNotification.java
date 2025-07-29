package com.amannmalik.mcp.jsonrpc;

import jakarta.json.JsonObject;


public record JsonRpcNotification(String method, JsonObject params) implements JsonRpcMessage {
    public JsonRpcNotification {
        if (method == null) {
            throw new IllegalArgumentException("method is required");
        }
    }

    @Override
    public String jsonrpc() {
        return JsonRpc.VERSION;
    }
}
