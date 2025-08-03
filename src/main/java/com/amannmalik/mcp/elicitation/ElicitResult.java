package com.amannmalik.mcp.elicitation;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.JsonUtil;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.Set;

public record ElicitResult(ElicitationAction action, JsonObject content, JsonObject _meta) {
    public static final JsonCodec<ElicitResult> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ElicitResult r) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("action", r.action().name().toLowerCase());
            if (r.content() != null) b.add("content", r.content());
            if (r._meta() != null) b.add("_meta", r._meta());
            return b.build();
        }

        @Override
        public ElicitResult fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("action required");
            JsonUtil.requireOnlyKeys(obj, Set.of("action", "content", "_meta"));
            String raw = obj.getString("action", null);
            if (raw == null) throw new IllegalArgumentException("action required");
            ElicitationAction action;
            try {
                action = ElicitationAction.valueOf(raw.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid action", e);
            }
            JsonValue c = obj.get("content");
            JsonObject content = null;
            if (c != null) {
                if (c.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("content must be object");
                }
                content = c.asJsonObject();
            }
            return new ElicitResult(action, content, obj.getJsonObject("_meta"));
        }
    };

    public ElicitResult {
        if (action == null) {
            throw new IllegalArgumentException("action is required");
        }
        if (action == ElicitationAction.ACCEPT && content == null) {
            throw new IllegalArgumentException("content required for ACCEPT action");
        }
        if (content != null) {
            for (var entry : content.entrySet()) {
                InputSanitizer.requireClean(entry.getKey());
                switch (entry.getValue().getValueType()) {
                    case STRING -> InputSanitizer.requireClean(content.getString(entry.getKey()));
                    case NUMBER, TRUE, FALSE -> {
                    }
                    default -> throw new IllegalArgumentException(
                            "content values must be primitive");
                }
            }
        }
        MetaValidator.requireValid(_meta);
    }
}
