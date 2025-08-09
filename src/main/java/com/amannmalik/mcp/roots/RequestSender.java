package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.jsonrpc.JsonRpcMessage;
import com.amannmalik.mcp.transport.RequestMethod;
import jakarta.json.JsonObject;

import java.io.IOException;

@FunctionalInterface
public interface RequestSender {
    JsonRpcMessage send(RequestMethod method, JsonObject params) throws IOException;
}

