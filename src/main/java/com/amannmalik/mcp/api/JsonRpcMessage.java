package com.amannmalik.mcp.api;

import com.amannmalik.mcp.jsonrpc.JsonRpc;
import com.amannmalik.mcp.jsonrpc.JsonRpcError;
import com.amannmalik.mcp.jsonrpc.JsonRpcNotification;
import com.amannmalik.mcp.jsonrpc.JsonRpcRequest;
import com.amannmalik.mcp.jsonrpc.JsonRpcResponse;

public sealed interface JsonRpcMessage permits JsonRpcRequest, JsonRpcNotification, JsonRpcResponse, JsonRpcError {
    default String jsonrpc() {
        return JsonRpc.VERSION;
    }
}
