package com.amannmalik.mcp.jsonrpc;

import java.io.IOException;

public final class JsonRpc {
    private JsonRpc() {
    }

    public static final String VERSION = "2.0";

    public static JsonRpcResponse expectResponse(JsonRpcMessage msg) throws IOException {
        if (msg instanceof JsonRpcResponse resp) return resp;
        if (msg instanceof JsonRpcError err) throw new IOException(err.error().message());
        throw new IOException("Unexpected message type: " + msg.getClass().getSimpleName());
    }
}
