package com.amannmalik.mcp.protocol.jsonrpc;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.util.Objects;
import java.util.Optional;

public record JsonRpcError(RequestId id, int code, String message, Optional<JsonValue> data) implements JsonRpcMessage {
    public JsonRpcError {
        Objects.requireNonNull(id);
        Objects.requireNonNull(message);
        Objects.requireNonNull(data);
    }

    @Override
    public JsonObject toJson() {
        var errorObj = Json.createObjectBuilder()
                .add("code", code)
                .add("message", message);
        data.ifPresent(d -> errorObj.add("data", d));
        return Json.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id.json())
                .add("error", errorObj.build())
                .build();
    }
}
