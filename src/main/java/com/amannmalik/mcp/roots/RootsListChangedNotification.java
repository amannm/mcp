package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.*;

public record RootsListChangedNotification() {
    public static final JsonCodec<RootsListChangedNotification> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(RootsListChangedNotification n) {
            if (n == null) throw new IllegalArgumentException("notification required");
            return JsonValue.EMPTY_JSON_OBJECT;
        }

        @Override
        public RootsListChangedNotification fromJson(JsonObject obj) {
            if (obj != null && !obj.isEmpty()) throw new IllegalArgumentException("unexpected fields");
            return new RootsListChangedNotification();
        }
    };
}
