package com.amannmalik.mcp.server;

import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.wire.RequestMethod;
import jakarta.json.JsonObject;

import java.io.IOException;

@FunctionalInterface
public interface RequestSender {
    JsonRpcMessage send(RequestMethod method, JsonObject params) throws IOException;
}

