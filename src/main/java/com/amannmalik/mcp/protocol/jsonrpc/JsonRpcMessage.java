package com.amannmalik.mcp.protocol.jsonrpc;

import jakarta.json.JsonObject;

public sealed interface JsonRpcMessage permits JsonRpcRequest, JsonRpcResponse, JsonRpcNotification, JsonRpcError {
    JsonObject toJson();
}
