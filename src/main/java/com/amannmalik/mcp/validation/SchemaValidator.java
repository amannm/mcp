package com.amannmalik.mcp.validation;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;


public final class SchemaValidator {
    private SchemaValidator() {}

    public static void validate(JsonObject schema, JsonObject value) {
        if (schema == null) return;
        if ("object".equals(schema.getString("type", null))) {
            JsonArray required = schema.getJsonArray("required");
            if (required != null) {
                for (JsonString r : required.getValuesAs(JsonString.class)) {
                    if (!value.containsKey(r.getString())) {
                        throw new IllegalArgumentException("Missing required field: " + r.getString());
                    }
                }
            }
            JsonObject props = schema.getJsonObject("properties");
            if (props != null) {
                for (var e : props.entrySet()) {
                    if (!value.containsKey(e.getKey())) continue;
                    String t = e.getValue().asJsonObject().getString("type", null);
                    if (t != null) {
                        JsonValue v = value.get(e.getKey());
                        switch (t) {
                            case "string" -> requireType(v, JsonValue.ValueType.STRING, e.getKey());
                            case "number" -> requireType(v, JsonValue.ValueType.NUMBER, e.getKey());
                            case "integer" -> requireType(v, JsonValue.ValueType.NUMBER, e.getKey());
                            case "boolean" -> requireType(v, JsonValue.ValueType.TRUE, JsonValue.ValueType.FALSE, e.getKey());
                            case "array" -> requireType(v, JsonValue.ValueType.ARRAY, e.getKey());
                            case "object" -> validate(e.getValue().asJsonObject(), value.getJsonObject(e.getKey()));
                            default -> {}
                        }
                    }
                }
            }
        }
    }

    private static void requireType(JsonValue value, JsonValue.ValueType expected, String field) {
        if (value.getValueType() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " for " + field);
        }
    }

    private static void requireType(JsonValue value, JsonValue.ValueType a, JsonValue.ValueType b, String field) {
        var vt = value.getValueType();
        if (vt != a && vt != b) {
            throw new IllegalArgumentException("Expected " + a + " or " + b + " for " + field);
        }
    }
}
