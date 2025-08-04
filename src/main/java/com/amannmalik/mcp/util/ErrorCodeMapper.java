package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.JsonRpcErrorCode;

import java.util.Map;

public final class ErrorCodeMapper {
    private static final Map<String, JsonRpcErrorCode> MAP = Map.of(
            "Parse Error", JsonRpcErrorCode.PARSE_ERROR,
            "Invalid Request", JsonRpcErrorCode.INVALID_REQUEST,
            "Method not found", JsonRpcErrorCode.METHOD_NOT_FOUND,
            "Invalid params", JsonRpcErrorCode.INVALID_PARAMS,
            "Lifecycle error", JsonRpcErrorCode.INVALID_REQUEST,
            "Internal error", JsonRpcErrorCode.INTERNAL_ERROR);

    private ErrorCodeMapper() {
    }

    public static int code(String message) {
        return MAP.getOrDefault(message, JsonRpcErrorCode.INVALID_REQUEST).code();
    }
}

