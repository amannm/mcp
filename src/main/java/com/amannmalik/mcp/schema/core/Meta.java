package com.amannmalik.mcp.schema.core;

import jakarta.json.JsonValue;

import java.util.Map;

/** Arbitrary metadata fields. */
public record Meta(Map<String, JsonValue> values) {
    public Meta {
        values = Map.copyOf(values);
    }

    public static final Meta EMPTY = new Meta(Map.of());
}
