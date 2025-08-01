package com.amannmalik.mcp.jsonrpc;

import jakarta.json.JsonValue;

public record JsonRpcError(RequestId id, ErrorDetail error) implements JsonRpcMessage {
    public record ErrorDetail(int code, String message, JsonValue data) {
    }

    public static JsonRpcError of(RequestId id, JsonRpcErrorCode code, String message) {
        return of(id, code, message, null);
    }

    public static JsonRpcError of(RequestId id, JsonRpcErrorCode code, String message, JsonValue data) {
        return new JsonRpcError(id, new ErrorDetail(code.code(), message, data));
    }

    public static JsonRpcError of(RequestId id, int code, String message) {
        return of(id, code, message, null);
    }

    public static JsonRpcError of(RequestId id, int code, String message, JsonValue data) {
        return new JsonRpcError(id, new ErrorDetail(code, message, data));
    }

    public static JsonRpcError invalidParams(RequestId id, String message) {
        return of(id, JsonRpcErrorCode.INVALID_PARAMS, message);
    }

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
