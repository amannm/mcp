package com.amannmalik.mcp.api;

import com.amannmalik.mcp.jsonrpc.*;

public sealed interface JsonRpcMessage permits
        JsonRpcRequest,
        JsonRpcNotification,
        JsonRpcResponse,
        JsonRpcError {
    default String jsonrpc() {
        return JsonRpc.VERSION;
    }
}
