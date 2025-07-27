package com.amannmalik.mcp.jsonrpc;

import jakarta.json.JsonValue;

/** An error response for a JSON-RPC request. */
public record JsonRpcError(RequestId id, ErrorDetail error) implements JsonRpcMessage {
    public record ErrorDetail(int code, String message, JsonValue data) {}

    public JsonRpcError {
        if (id == null || error == null) {
            throw new IllegalArgumentException("id and error are required");
        }
    }

    @Override
    public String jsonrpc() {
        return JsonRpc.VERSION;
    }
}
