package com.amannmalik.mcp.server.roots.validation;

import jakarta.json.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class SchemaValidator {
    private SchemaValidator() {
    }

    public static void validate(JsonObject schema, JsonObject value) {
        if (schema == null) return;
        if (!"object".equals(schema.getString("type", null))) return;
        validateObject(schema, value);
    }

    private static void validateObject(JsonObject schema, JsonObject value) {
        JsonArray required = schema.getJsonArray("required");
        if (required != null) {
            for (JsonString r : required.getValuesAs(JsonString.class)) {
                if (!value.containsKey(r.getString())) {
                    throw new IllegalArgumentException("Missing required field: " + r.getString());
                }
            }
        }
        JsonObject props = schema.getJsonObject("properties");
        if (props == null) return;
        for (var e : props.entrySet()) {
            String name = e.getKey();
            if (!value.containsKey(name)) continue;
            JsonObject prop = e.getValue().asJsonObject();
            JsonValue v = value.get(name);
            validateProperty(name, prop, v);
        }
        for (String key : value.keySet()) {
            if (!props.containsKey(key)) {
                throw new IllegalArgumentException("Unexpected field: " + key);
            }
        }
    }

    private static void validateProperty(String name, JsonObject schema, JsonValue value) {
        String t = schema.getString("type", null);
        if (t == null) return;
        switch (t) {
            case "string" -> validateString(name, value, schema);
            case "number" -> validateNumber(name, value, schema);
            case "integer" -> validateInteger(name, value, schema);
            case "boolean" -> requireType(value, JsonValue.ValueType.TRUE, JsonValue.ValueType.FALSE, name);
            case "array" -> requireType(value, JsonValue.ValueType.ARRAY, name);
            case "object" -> {
                requireType(value, JsonValue.ValueType.OBJECT, name);
                validate(schema, value.asJsonObject());
            }
            default -> {
            }
        }
    }

    private static void validateString(String field, JsonValue v, JsonObject schema) {
        requireType(v, JsonValue.ValueType.STRING, field);
        String str = ((JsonString) v).getString();
        if (schema.containsKey("minLength") && str.length() < schema.getInt("minLength")) {
            throw new IllegalArgumentException("minLength violated for " + field);
        }
        if (schema.containsKey("maxLength") && str.length() > schema.getInt("maxLength")) {
            throw new IllegalArgumentException("maxLength violated for " + field);
        }
        if (schema.containsKey("enum")) {
            boolean match = schema.getJsonArray("enum")
                    .getValuesAs(JsonString.class)
                    .stream()
                    .anyMatch(js -> js.getString().equals(str));
            if (!match) {
                throw new IllegalArgumentException("enum violation for " + field);
            }
        }
        if (schema.containsKey("format")) {
            validateFormat(str, schema.getString("format"), field);
        }
    }

    private static void validateNumber(String field, JsonValue v, JsonObject schema) {
        requireType(v, JsonValue.ValueType.NUMBER, field);
        double num = ((JsonNumber) v).doubleValue();
        if (schema.containsKey("minimum") && num < schema.getJsonNumber("minimum").doubleValue()) {
            throw new IllegalArgumentException("minimum violated for " + field);
        }
        if (schema.containsKey("maximum") && num > schema.getJsonNumber("maximum").doubleValue()) {
            throw new IllegalArgumentException("maximum violated for " + field);
        }
    }

    private static void validateInteger(String field, JsonValue v, JsonObject schema) {
        requireInteger(v, field);
        long num = ((JsonNumber) v).longValue();
        if (schema.containsKey("minimum") && num < schema.getJsonNumber("minimum").longValue()) {
            throw new IllegalArgumentException("minimum violated for " + field);
        }
        if (schema.containsKey("maximum") && num > schema.getJsonNumber("maximum").longValue()) {
            throw new IllegalArgumentException("maximum violated for " + field);
        }
    }

    private static void validateFormat(String value, String format, String field) {
        try {
            switch (format) {
                case "email" -> {
                    if (!value.matches("^[^@]+@[^@]+\\.[^@]+$")) {
                        throw new IllegalArgumentException("Invalid email for " + field);
                    }
                }
                case "uri" -> {
                    if (URI.create(value).toString().isEmpty()) {
                        throw new IllegalArgumentException("Invalid uri for " + field);
                    }
                }
                case "date" -> LocalDate.parse(value);
                case "date-time" -> OffsetDateTime.parse(value);
                default -> {
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid " + format + " for " + field);
        }
    }

    private static void requireType(JsonValue value, JsonValue.ValueType expected, String field) {
        if (value.getValueType() != expected) {
            throw new IllegalArgumentException("Expected " + expected + " for " + field);
        }
    }

    private static void requireInteger(JsonValue value, String field) {
        if (value.getValueType() != JsonValue.ValueType.NUMBER
                || !((JsonNumber) value).isIntegral()) {
            throw new IllegalArgumentException("Expected integer for " + field);
        }
    }

    private static void requireType(JsonValue value, JsonValue.ValueType a, JsonValue.ValueType b, String field) {
        var vt = value.getValueType();
        if (vt != a && vt != b) {
            throw new IllegalArgumentException("Expected " + a + " or " + b + " for " + field);
        }
    }
}
