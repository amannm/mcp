package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.*;

public record ToolListChangedNotification() {
    public static final JsonCodec<ToolListChangedNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(ToolListChangedNotification n) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }

        @Override
        public ToolListChangedNotification fromJson(JsonObject obj) {
            if (obj != null && !obj.isEmpty()) throw new IllegalArgumentException("unexpected fields");
            return new ToolListChangedNotification();
        }
    };
}
