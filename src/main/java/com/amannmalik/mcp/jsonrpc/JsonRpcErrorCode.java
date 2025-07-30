package com.amannmalik.mcp.jsonrpc;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum JsonRpcErrorCode {
    PARSE_ERROR(-32700),
    INVALID_REQUEST(-32600),
    METHOD_NOT_FOUND(-32601),
    INVALID_PARAMS(-32602),
    INTERNAL_ERROR(-32603);

    private static final Map<Integer, JsonRpcErrorCode> BY_CODE;
    private final int code;

    JsonRpcErrorCode(int code) {
        this.code = code;
    }

    static {
        BY_CODE = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(e -> e.code, e -> e));
    }

    public int code() {
        return code;
    }

    public static JsonRpcErrorCode fromCode(int code) {
        JsonRpcErrorCode e = BY_CODE.get(code);
        if (e == null) throw new IllegalArgumentException("Unknown error code: " + code);
        return e;
    }
}
