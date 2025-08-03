package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.EmptyJsonObjectCodec;
import jakarta.json.JsonObject;

public record ToolListChangedNotification() {
    public static final JsonCodec<ToolListChangedNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ToolListChangedNotification n) {
            return EmptyJsonObjectCodec.toJsonObject();
        }

        @Override
        public ToolListChangedNotification fromJson(JsonObject obj) {
            EmptyJsonObjectCodec.requireEmpty(obj);
            return new ToolListChangedNotification();
        }
    };
}
