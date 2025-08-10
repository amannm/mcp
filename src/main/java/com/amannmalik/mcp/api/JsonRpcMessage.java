package com.amannmalik.mcp.api;

import com.amannmalik.mcp.jsonrpc.*;

sealed public interface JsonRpcMessage permits JsonRpcRequest, JsonRpcNotification, JsonRpcResponse, JsonRpcError {
    String jsonrpc();
}
