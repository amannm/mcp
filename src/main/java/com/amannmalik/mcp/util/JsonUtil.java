package com.amannmalik.mcp.util;

import jakarta.json.JsonObject;

import java.util.Set;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static void requireOnlyKeys(JsonObject obj, Set<String> allowed) {
        if (obj == null || allowed == null) return;
        for (String key : obj.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("unexpected field: " + key);
            }
        }
    }
}
