package com.amannmalik.mcp.util;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/**
 * Utility for encoding and validating empty JSON objects.
 */
public final class EmptyJsonObjectCodec {
    private EmptyJsonObjectCodec() {
    }

    /**
     * Returns an empty JSON object instance.
     */
    public static JsonObject toJsonObject() {
        return JsonValue.EMPTY_JSON_OBJECT;
    }

    /**
     * Ensures the provided object is either {@code null} or empty.
     *
     * @throws IllegalArgumentException if the object contains any fields
     */
    public static void requireEmpty(JsonObject obj) {
        if (obj != null && !obj.isEmpty()) {
            throw new IllegalArgumentException("unexpected fields");
        }
    }
}
