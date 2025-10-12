package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.JsonRpcMessage;
import com.amannmalik.mcp.api.RequestMethod;
import jakarta.json.JsonObject;

import java.io.IOException;
import java.time.Duration;

@FunctionalInterface
public interface RequestSender {
    JsonRpcMessage send(RequestMethod method, JsonObject params, Duration timeout) throws IOException;
}
