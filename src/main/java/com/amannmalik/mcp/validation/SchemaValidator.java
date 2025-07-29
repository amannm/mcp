package com.amannmalik.mcp.validation;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

public final class SchemaValidator {
    private SchemaValidator() {
    }

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
                    String name = e.getKey();
                    JsonObject prop = e.getValue().asJsonObject();
                    if (!value.containsKey(name)) continue;
                    String t = prop.getString("type", null);
                    if (t != null) {
                        JsonValue v = value.get(name);
                        switch (t) {
                            case "string" -> {
                                requireType(v, JsonValue.ValueType.STRING, name);
                                String str = ((JsonString) v).getString();
                                if (prop.containsKey("minLength") && str.length() < prop.getInt("minLength")) {
                                    throw new IllegalArgumentException("minLength violated for " + name);
                                }
                                if (prop.containsKey("maxLength") && str.length() > prop.getInt("maxLength")) {
                                    throw new IllegalArgumentException("maxLength violated for " + name);
                                }
                                if (prop.containsKey("enum")) {
                                    boolean match = prop.getJsonArray("enum")
                                            .getValuesAs(JsonString.class)
                                            .stream()
                                            .anyMatch(js -> js.getString().equals(str));
                                    if (!match) {
                                        throw new IllegalArgumentException("enum violation for " + name);
                                    }
                                }
                                if (prop.containsKey("format")) {
                                    String fmt = prop.getString("format");
                                    validateFormat(str, fmt, name);
                                }
                            }
                            case "number" -> {
                                requireType(v, JsonValue.ValueType.NUMBER, name);
                                double num = ((JsonNumber) v).doubleValue();
                                if (prop.containsKey("minimum") && num < prop.getJsonNumber("minimum").doubleValue()) {
                                    throw new IllegalArgumentException("minimum violated for " + name);
                                }
                                if (prop.containsKey("maximum") && num > prop.getJsonNumber("maximum").doubleValue()) {
                                    throw new IllegalArgumentException("maximum violated for " + name);
                                }
                            }
                            case "integer" -> {
                                requireInteger(v, name);
                                long num = ((JsonNumber) v).longValue();
                                if (prop.containsKey("minimum") && num < prop.getJsonNumber("minimum").longValue()) {
                                    throw new IllegalArgumentException("minimum violated for " + name);
                                }
                                if (prop.containsKey("maximum") && num > prop.getJsonNumber("maximum").longValue()) {
                                    throw new IllegalArgumentException("maximum violated for " + name);
                                }
                            }
                            case "boolean" -> requireType(v, JsonValue.ValueType.TRUE, JsonValue.ValueType.FALSE, name);
                            case "array" -> requireType(v, JsonValue.ValueType.ARRAY, name);
                            case "object" -> validate(prop, value.getJsonObject(name));
                            default -> {
                            }
                        }
                    }
                }
            }
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
                case "uri" -> java.net.URI.create(value);
                case "date" -> java.time.LocalDate.parse(value);
                case "date-time" -> java.time.OffsetDateTime.parse(value);
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
