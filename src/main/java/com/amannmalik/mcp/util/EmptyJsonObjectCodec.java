package com.amannmalik.mcp.util;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public final class EmptyJsonObjectCodec {
    private EmptyJsonObjectCodec() {
    }

    public static JsonObject toJsonObject() {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    public static void requireEmpty(JsonObject obj) {
        if (obj != null && !obj.isEmpty()) {
            throw new IllegalArgumentException("unexpected fields");
        }
    }
}
