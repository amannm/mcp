package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.EmptyJsonObjectCodec;
import jakarta.json.JsonObject;

public record PromptListChangedNotification() {
    public static final JsonCodec<PromptListChangedNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(PromptListChangedNotification n) {
            return EmptyJsonObjectCodec.toJsonObject();
        }

        @Override
        public PromptListChangedNotification fromJson(JsonObject obj) {
            EmptyJsonObjectCodec.requireEmpty(obj);
            return new PromptListChangedNotification();
        }
    };
}
