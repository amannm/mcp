package com.amannmalik.mcp.schema.definitions;

import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.List;
import java.util.Map;

/** Basic validation helpers for schema definitions. */
public final class SchemaValidator {
    private SchemaValidator() {}

    public static boolean validate(JsonValue value, PrimitiveSchemaDefinition schema) {
        return switch (schema) {
            case StringSchema s -> validateString(value, s);
            case NumberSchema n -> validateNumber(value, n);
            case BooleanSchema b -> validateBoolean(value, b);
            case EnumSchema e -> validateEnum(value, e);
        };
    }

    public static boolean validate(JsonObject obj, ObjectSchema schema) {
        for (var req : schema.required().orElse(List.of())) if (!obj.containsKey(req)) return false;
        for (Map.Entry<String, PrimitiveSchemaDefinition> e : schema.properties().entrySet()) {
            if (obj.containsKey(e.getKey()) && !validate(obj.get(e.getKey()), e.getValue())) return false;
        }
        return true;
    }

    private static boolean validateString(JsonValue v, StringSchema s) {
        if (!(v instanceof JsonString js)) return false;
        int len = js.getString().length();
        if (s.minLength().isPresent() && len < s.minLength().get()) return false;
        if (s.maxLength().isPresent() && len > s.maxLength().get()) return false;
        return true;
    }

    private static boolean validateNumber(JsonValue v, NumberSchema s) {
        if (!(v instanceof JsonNumber jn)) return false;
        double num = jn.doubleValue();
        if (s.type() == NumberSchema.Type.INTEGER && num != Math.rint(num)) return false;
        if (s.minimum().isPresent() && num < s.minimum().get()) return false;
        if (s.maximum().isPresent() && num > s.maximum().get()) return false;
        return true;
    }

    private static boolean validateBoolean(JsonValue v, BooleanSchema s) {
        return v == JsonValue.TRUE || v == JsonValue.FALSE;
    }

    private static boolean validateEnum(JsonValue v, EnumSchema e) {
        return v instanceof JsonString js && e.values().contains(js.getString());
    }
}
