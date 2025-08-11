package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.model.RequestMethod;
import jakarta.json.JsonObject;

import java.io.IOException;

@FunctionalInterface
public interface RequestSender {
    JsonRpcMessage send(RequestMethod method, JsonObject params, long timeoutMillis) throws IOException;
}

