package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;
import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public final class CancellationCodec {
    private CancellationCodec() {
    }

    public static JsonObject toJsonObject(CancelledNotification note) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        switch (note.requestId()) {
            case RequestId.StringId s -> b.add("requestId", s.value());
            case RequestId.NumericId n -> b.add("requestId", n.value());
            default -> throw new IllegalArgumentException("Invalid requestId type");
        }
        if (note.reason() != null) b.add("reason", note.reason());
        return b.build();
    }

    public static CancelledNotification toCancelledNotification(JsonObject obj) {
        RequestId id = toId(obj.get("requestId"));
        String reason = obj.getString("reason", null);
        return new CancelledNotification(id, reason);
    }

    private static RequestId toId(JsonValue v) {
        if (v == null || v.getValueType() == JsonValue.ValueType.NULL) {
            throw new IllegalArgumentException("requestId is required");
        }
        return switch (v.getValueType()) {
            case STRING -> new RequestId.StringId(((JsonString) v).getString());
            case NUMBER -> new RequestId.NumericId(((JsonNumber) v).longValue());
            default -> throw new IllegalArgumentException("Invalid requestId type");
        };
    }
}
