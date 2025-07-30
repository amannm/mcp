package com.amannmalik.mcp.client.elicitation;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

public final class ElicitCodec {
    private ElicitCodec() {
    }

    public static JsonObject toJsonObject(ElicitRequest req) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("message", req.message())
                .add("requestedSchema", req.requestedSchema());
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    public static ElicitRequest toRequest(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        if (!obj.containsKey("message")) throw new IllegalArgumentException("message required");
        if (!obj.containsKey("requestedSchema")) throw new IllegalArgumentException("requestedSchema required");
        var schemaVal = obj.get("requestedSchema");
        if (schemaVal == null || schemaVal.getValueType() != jakarta.json.JsonValue.ValueType.OBJECT) {
            throw new IllegalArgumentException("requestedSchema must be object");
        }
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new ElicitRequest(
                obj.getString("message"),
                schemaVal.asJsonObject(),
                meta
        );
    }

    public static JsonObject toJsonObject(ElicitResult resp) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("action", resp.action().name().toLowerCase());
        if (resp.content() != null) b.add("content", resp.content());
        if (resp._meta() != null) b.add("_meta", resp._meta());
        return b.build();
    }

    public static ElicitResult toResult(JsonObject obj) {
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
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new ElicitResult(action, content, meta);
    }
}
