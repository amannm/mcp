package com.amannmalik.mcp.client.elicitation;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

public final class ElicitationCodec {
    private ElicitationCodec() {
    }

    public static JsonObject toJsonObject(ElicitationRequest req) {
        return Json.createObjectBuilder()
                .add("message", req.message())
                .add("requestedSchema", req.requestedSchema())
                .build();
    }

    public static ElicitationRequest toRequest(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        if (!obj.containsKey("message")) throw new IllegalArgumentException("message required");
        if (!obj.containsKey("requestedSchema")) throw new IllegalArgumentException("requestedSchema required");
        var schemaVal = obj.get("requestedSchema");
        if (schemaVal == null || schemaVal.getValueType() != jakarta.json.JsonValue.ValueType.OBJECT) {
            throw new IllegalArgumentException("requestedSchema must be object");
        }
        return new ElicitationRequest(
                obj.getString("message"),
                schemaVal.asJsonObject()
        );
    }

    public static JsonObject toJsonObject(ElicitationResponse resp) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("action", resp.action().name().toLowerCase());
        if (resp.content() != null) b.add("content", resp.content());
        return b.build();
    }

    public static ElicitationResponse toResponse(JsonObject obj) {
        if (obj == null || !obj.containsKey("action")) {
            throw new IllegalArgumentException("action required");
        }
        String raw = obj.getString("action", null);
        if (raw == null) throw new IllegalArgumentException("action required");
        ElicitationAction action;
        try {
            action = ElicitationAction.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid action", e);
        }
        jakarta.json.JsonValue c = obj.get("content");
        JsonObject content = null;
        if (c != null) {
            if (c.getValueType() != jakarta.json.JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("content must be object");
            }
            content = c.asJsonObject();
        }
        return new ElicitationResponse(action, content);
    }
}
