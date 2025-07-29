package com.amannmalik.mcp.jsonrpc;

public sealed interface JsonRpcMessage permits JsonRpcRequest, JsonRpcNotification, JsonRpcResponse, JsonRpcError {
    String jsonrpc();
}
