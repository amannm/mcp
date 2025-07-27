package com.amannmalik.mcp.json;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import java.util.Map;
import java.util.Optional;

public final class McpJsonWriter {
    private static final JsonBuilderFactory FACTORY = Json.createBuilderFactory(Map.of());

    private McpJsonWriter() {}

    public static JsonObjectBuilder object() {
        return FACTORY.createObjectBuilder();
    }

    public static JsonArrayBuilder array() {
        return FACTORY.createArrayBuilder();
    }

    public static <T> void add(JsonObjectBuilder b, String k, Optional<T> v, JsonCodec<T> c) {
        v.ifPresent(value -> b.add(k, c.toJson(value)));
    }

    public static void add(JsonObjectBuilder b, String k, Optional<? extends JsonValue> v) {
        v.ifPresent(value -> b.add(k, value));
    }

    public static void addBase64(JsonObjectBuilder b, String k, Optional<byte[]> v) {
        v.ifPresent(bytes -> b.add(k, Base64Util.encode(bytes)));
    }

    public static void addUnion(JsonObjectBuilder b, String k, Optional<?> v) {
        v.ifPresent(value -> b.add(k, JsonRegistry.toUnion(value)));
    }
}
