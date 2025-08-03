package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.EmptyJsonObjectCodec;
import jakarta.json.JsonObject;

public record ResourceListChangedNotification() {
    public static final JsonCodec<ResourceListChangedNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ResourceListChangedNotification n) {
            return EmptyJsonObjectCodec.toJsonObject();
        }

        @Override
        public ResourceListChangedNotification fromJson(JsonObject obj) {
            EmptyJsonObjectCodec.requireEmpty(obj);
            return new ResourceListChangedNotification();
        }
    };
}
