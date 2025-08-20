package com.amannmalik.mcp.jsonrpc;

import com.amannmalik.mcp.api.JsonRpcMessage;

import java.io.IOException;

/// - [Base Protocol](specification/2025-06-18/basic/index.mdx)
/// - [MCP server conformance test](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:6-34)
public final class JsonRpc {
    public static final String VERSION = "2.0";

    private JsonRpc() {
    }

    public static JsonRpcResponse expectResponse(JsonRpcMessage msg) throws IOException {
        if (msg instanceof JsonRpcResponse resp) {
            return resp;
        }
        if (msg instanceof JsonRpcError err) {
            throw new IOException(err.error().message());
        }
        throw new IOException("Unexpected message type: " + msg.getClass().getSimpleName());
    }
}
