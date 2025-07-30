package com.amannmalik.mcp.jsonrpc;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public final class JsonRpcCodec {
    private JsonRpcCodec() {
    }

    public static JsonObject toJsonObject(JsonRpcMessage msg) {
        var builder = Json.createObjectBuilder();
        builder.add("jsonrpc", JsonRpc.VERSION);

        switch (msg) {
            case JsonRpcRequest r -> {
                addId(builder, r.id());
                builder.add("method", r.method());
                if (r.params() != null) builder.add("params", r.params());
            }
            case JsonRpcNotification n -> {
                builder.add("method", n.method());
                if (n.params() != null) builder.add("params", n.params());
            }
            case JsonRpcResponse r -> {
                addId(builder, r.id());
                builder.add("result", r.result() != null ? r.result() : JsonValue.NULL);
            }
            case JsonRpcError e -> {
                addId(builder, e.id());
                var err = e.error();
                var errBuilder = Json.createObjectBuilder()
                        .add("code", err.code())
                        .add("message", err.message());
                if (err.data() != null) errBuilder.add("data", err.data());
                builder.add("error", errBuilder.build());
            }
        }
        return builder.build();
    }

    private static void addId(JsonObjectBuilder builder, RequestId id) {
        switch (id) {
            case RequestId.StringId s -> builder.add("id", s.value());
            case RequestId.NumericId n -> builder.add("id", n.value());
        }
    }

    public static JsonRpcMessage fromJsonObject(JsonObject obj) {
        var version = obj.getString("jsonrpc", null);
        if (!JsonRpc.VERSION.equals(version)) {
            throw new IllegalArgumentException("Unsupported jsonrpc version: " + version);
        }

        var idValue = obj.get("id");
        var method = obj.getString("method", null);
        var hasError = obj.containsKey("error");
        var hasResult = obj.containsKey("result");

        if (method != null && idValue != null && idValue.getValueType() != JsonValue.ValueType.NULL) {
            return new JsonRpcRequest(toId(idValue), method, obj.getJsonObject("params"));
        }
        if (method != null) {
            return new JsonRpcNotification(method, obj.getJsonObject("params"));
        }
        if (hasResult) {
            if (idValue == null || idValue.getValueType() == JsonValue.ValueType.NULL) {
                throw new IllegalArgumentException("id is required for response");
            }
            return new JsonRpcResponse(toId(idValue), obj.getJsonObject("result"));
        }
        if (hasError) {
            if (idValue == null || idValue.getValueType() == JsonValue.ValueType.NULL) {
                throw new IllegalArgumentException("id is required for error");
            }
            var errObj = obj.getJsonObject("error");
            var detail = new JsonRpcError.ErrorDetail(
                    errObj.getInt("code"),
                    errObj.getString("message"),
                    errObj.get("data")
            );
            return new JsonRpcError(toId(idValue), detail);
        }
        throw new IllegalArgumentException("Unknown message type");
    }

    private static RequestId toId(JsonValue value) {
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            throw new IllegalArgumentException("id is required");
        }
        return switch (value.getValueType()) {
            case NUMBER -> new RequestId.NumericId(((JsonNumber) value).doubleValue());
            case STRING -> new RequestId.StringId(((JsonString) value).getString());
            default -> throw new IllegalArgumentException("Invalid id type");
        };
    }
}
