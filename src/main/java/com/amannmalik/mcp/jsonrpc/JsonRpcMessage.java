package com.amannmalik.mcp.jsonrpc;

/** Base marker for all JSON-RPC messages. */
public sealed interface JsonRpcMessage permits JsonRpcRequest, JsonRpcNotification, JsonRpcResponse, JsonRpcError {
    String jsonrpc();
}
