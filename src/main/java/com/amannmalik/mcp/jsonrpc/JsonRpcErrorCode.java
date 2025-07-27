package com.amannmalik.mcp.jsonrpc;

/** Standard JSON-RPC error codes. */
public enum JsonRpcErrorCode {
    PARSE_ERROR(-32700),
    INVALID_REQUEST(-32600),
    METHOD_NOT_FOUND(-32601),
    INVALID_PARAMS(-32602),
    INTERNAL_ERROR(-32603);

    private final int code;

    JsonRpcErrorCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static JsonRpcErrorCode fromCode(int code) {
        for (var c : values()) {
            if (c.code == code) return c;
        }
        throw new IllegalArgumentException("Unknown error code: " + code);
    }
}
