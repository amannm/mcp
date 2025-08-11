package com.amannmalik.mcp.jsonrpc;

import java.util.*;
import java.util.stream.Collectors;

public enum JsonRpcErrorCode {
    PARSE_ERROR(-32700),
    INVALID_REQUEST(-32600),
    METHOD_NOT_FOUND(-32601),
    INVALID_PARAMS(-32602),
    INTERNAL_ERROR(-32603),
    LIFECYCLE_ERROR(0);

    private static final Map<Integer, JsonRpcErrorCode> BY_CODE;

    static {
        BY_CODE = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(e -> e.code, e -> e));
    }

    private final int code;

    JsonRpcErrorCode(int code) {
        this.code = code;
    }

    public static Optional<JsonRpcErrorCode> fromCode(int code) {
        return Optional.ofNullable(BY_CODE.get(code));
    }

    public int code() {
        return code;
    }
}
