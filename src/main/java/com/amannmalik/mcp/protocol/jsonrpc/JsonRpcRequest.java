package com.amannmalik.mcp.protocol.jsonrpc;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.util.Optional;

public record JsonRpcRequest(RequestId id, String method, Optional<JsonValue> params) implements JsonRpcMessage {
    public JsonRpcRequest(RequestId id, String method, Optional<JsonValue> params) {
        this.id = id;
        this.method = method;
        this.params = params == null ? Optional.empty() : params;
    }

    @Override
    public JsonObject toJson() {
        var builder = Json.createObjectBuilder().add("jsonrpc", "2.0").add("id", id.json()).add("method", method);
        params.ifPresent(p -> builder.add("params", p));
        return builder.build();
    }
}
