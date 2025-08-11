package com.amannmalik.mcp.jsonrpc;

import com.amannmalik.mcp.api.JsonRpcMessage;
import jakarta.json.JsonObject;

public record JsonRpcNotification(String method, JsonObject params) implements JsonRpcMessage {
    public JsonRpcNotification {
        if (method == null) {
            throw new IllegalArgumentException("method is required");
        }
    }
}
