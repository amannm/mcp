package com.amannmalik.mcp.json;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.JsonObject;

import java.util.Optional;

public record McpJsonReader(JsonValue value) {
    public Optional<JsonObject> asObject() {
        return value instanceof JsonObject o ? Optional.of(o) : Optional.empty();
    }

    public Optional<String> asString() {
        return value instanceof JsonString s ? Optional.of(s.getString()) : Optional.empty();
    }

    public Optional<String> field(String name) {
        return asObject().map(o -> o.get(name)).flatMap(v -> new McpJsonReader(v).asString());
    }

    public Optional<byte[]> base64Field(String name) {
        return field(name).map(Base64Util::decode);
    }

    public <T> Optional<T> field(String name, Class<T> type) {
        return asObject().map(o -> o.get(name)).flatMap(v -> JsonRegistry.codec(type).map(c -> c.fromJson(v)));
    }

    public <T> Optional<T> unionField(String name) {
        return asObject().map(o -> o.getJsonObject(name)).flatMap(JsonRegistry::fromUnion);
    }
}
