package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.api.RequestMethod;
import com.amannmalik.mcp.api.JsonRpcMessage;
import jakarta.json.JsonObject;

import java.io.IOException;

@FunctionalInterface
public interface RequestSender {
    JsonRpcMessage send(RequestMethod method, JsonObject params) throws IOException;
}

