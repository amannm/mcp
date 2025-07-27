package com.amannmalik.mcp.protocol.jsonrpc;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Objects;
import java.util.Optional;

public record JsonRpcNotification(String method, Optional<JsonValue> params) implements JsonRpcMessage {

    public JsonRpcNotification {
        Objects.requireNonNull(method);
        Objects.requireNonNull(params);
    }

    @Override
    public JsonObject toJson() {
        var builder = Json.createObjectBuilder().add("jsonrpc", "2.0").add("method", method);
        params.ifPresent(p -> builder.add("params", p));
        return builder.build();
    }
}
