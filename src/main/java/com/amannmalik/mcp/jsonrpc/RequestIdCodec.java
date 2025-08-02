package com.amannmalik.mcp.jsonrpc;

import jakarta.json.*;

public final class RequestIdCodec {
    private RequestIdCodec() {
    }

    public static void add(JsonObjectBuilder builder, String key, RequestId id) {
        builder.add(key, toJsonValue(id));
    }

    public static JsonValue toJsonValue(RequestId id) {
        return switch (id) {
            case RequestId.StringId s -> Json.createValue(s.value());
            case RequestId.NumericId n -> Json.createValue(n.value());
            case RequestId.NullId ignored -> JsonValue.NULL;
        };
    }

    public static RequestId from(JsonValue value) {
        if (value == null || value.getValueType() == JsonValue.ValueType.NULL) {
            throw new IllegalArgumentException("id is required");
        }
        return switch (value.getValueType()) {
            case STRING -> new RequestId.StringId(((JsonString) value).getString());
            case NUMBER -> new RequestId.NumericId(((JsonNumber) value).longValue());
            default -> throw new IllegalArgumentException("Invalid id type");
        };
    }
}
