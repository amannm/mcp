package com.amannmalik.mcp.protocol.jsonrpc;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Objects;

public record JsonRpcResponse(RequestId id, JsonValue result) implements JsonRpcMessage {
    public JsonRpcResponse {
        Objects.requireNonNull(id);
        Objects.requireNonNull(result);
    }
    @Override
    public JsonObject toJson() {
        return Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id.json())
                .add("result", result)
                .build();
    }
}
