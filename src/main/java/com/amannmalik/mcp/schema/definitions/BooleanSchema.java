package com.amannmalik.mcp.schema.definitions;

import com.amannmalik.mcp.json.McpJsonWriter;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.json.JsonObjectBuilder;
import java.util.Optional;

/** JSON boolean schema definition. */
public record BooleanSchema(
        Optional<String> title,
        Optional<String> description,
        Optional<Boolean> defaultValue
) implements PrimitiveSchemaDefinition {

    @Override
    public JsonObject json() {
        JsonObjectBuilder b = McpJsonWriter.object().add("type", "boolean");
        McpJsonWriter.add(b, "title", title.map(Json::createValue));
        McpJsonWriter.add(b, "description", description.map(Json::createValue));
        McpJsonWriter.add(b, "default", defaultValue.map(v -> v ? JsonValue.TRUE : JsonValue.FALSE));
        return b.build();
    }

    static BooleanSchema fromJson(JsonObject obj) {
        return new BooleanSchema(
                Optional.ofNullable(obj.getString("title", null)),
                Optional.ofNullable(obj.getString("description", null)),
                obj.containsKey("default") ? Optional.of(obj.getBoolean("default")) : Optional.empty()
        );
    }
}
