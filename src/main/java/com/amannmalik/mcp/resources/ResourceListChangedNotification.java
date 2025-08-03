package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.*;

public record ResourceListChangedNotification() {
    public static final JsonCodec<ResourceListChangedNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ResourceListChangedNotification n) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }

        @Override
        public ResourceListChangedNotification fromJson(JsonObject obj) {
            if (obj != null && !obj.isEmpty()) throw new IllegalArgumentException("unexpected fields");
            return new ResourceListChangedNotification();
        }
    };
}
