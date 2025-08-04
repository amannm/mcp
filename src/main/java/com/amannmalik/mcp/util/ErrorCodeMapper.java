package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;

import java.util.Map;

public final class ErrorCodeMapper {
    private static final Map<String, JsonRpcErrorCode> MAP = Map.of(
            "Parse Error", JsonRpcErrorCode.PARSE_ERROR,
            "Invalid Request", JsonRpcErrorCode.INVALID_REQUEST,
            "Method not found", JsonRpcErrorCode.METHOD_NOT_FOUND,
            "Invalid params", JsonRpcErrorCode.INVALID_PARAMS,
            "Internal error", JsonRpcErrorCode.INTERNAL_ERROR,
            "Lifecycle error", JsonRpcErrorCode.LIFECYCLE_ERROR);

    private ErrorCodeMapper() {
    }

    public static int code(String message) {
        JsonRpcErrorCode code = MAP.get(message);
        if (code == null) throw new IllegalArgumentException(message);
        return code.code();
    }
}

