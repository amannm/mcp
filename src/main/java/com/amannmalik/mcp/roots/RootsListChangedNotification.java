package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.EmptyJsonObjectCodec;
import jakarta.json.JsonObject;

public record RootsListChangedNotification() {
    public static final JsonCodec<RootsListChangedNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(RootsListChangedNotification n) {
            if (n == null) throw new IllegalArgumentException("notification required");
            return EmptyJsonObjectCodec.toJsonObject();
        }

        @Override
        public RootsListChangedNotification fromJson(JsonObject obj) {
            EmptyJsonObjectCodec.requireEmpty(obj);
            return new RootsListChangedNotification();
        }
    };
}
