package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.*;

public record PromptListChangedNotification() {
    public static final JsonCodec<PromptListChangedNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(PromptListChangedNotification n) {
            return JsonValue.EMPTY_JSON_OBJECT;
        }

        @Override
        public PromptListChangedNotification fromJson(JsonObject obj) {
            if (obj != null && !obj.isEmpty()) throw new IllegalArgumentException("unexpected fields");
            return new PromptListChangedNotification();
        }
    };
}
