package com.amannmalik.mcp.jsonrpc;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.RequestId;
import jakarta.json.JsonValue;

public record JsonRpcError(RequestId id, ErrorDetail error) implements JsonRpcMessage {
    public JsonRpcError {
        if (id == null || error == null) {
            throw new IllegalArgumentException("id and error are required");
        }
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

    public record ErrorDetail(int code, String message, JsonValue data) {
        public ErrorDetail {
            if (message == null) {
                throw new IllegalArgumentException("message is required");
            }
        }
    }
}
