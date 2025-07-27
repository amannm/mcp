package com.amannmalik.mcp.schema.definitions;

import jakarta.json.JsonObject;
import java.util.Optional;

/** Restricted schema definition consisting of primitive types. */
public sealed interface PrimitiveSchemaDefinition permits StringSchema, NumberSchema, BooleanSchema, EnumSchema {
    JsonObject json();

    static Optional<PrimitiveSchemaDefinition> fromJson(JsonObject obj) {
        var type = obj.getString("type", null);
        if (type == null) return Optional.empty();
        return switch (type) {
            case "string" -> obj.containsKey("enum")
                    ? Optional.of(EnumSchema.fromJson(obj))
                    : Optional.of(StringSchema.fromJson(obj));
            case "number", "integer" -> Optional.of(NumberSchema.fromJson(obj));
            case "boolean" -> Optional.of(BooleanSchema.fromJson(obj));
            default -> Optional.empty();
        };
    }
}
