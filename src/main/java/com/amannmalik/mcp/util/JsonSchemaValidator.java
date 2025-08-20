package com.amannmalik.mcp.util;

import jakarta.json.*;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class JsonSchemaValidator {
    private JsonSchemaValidator() {
    }

    public static void validate(JsonObject schema, JsonObject value) {
        if (schema == null) {
            return;
        }
        if (!"object".equals(schema.getString("type", null))) {
            return;
        }
        validateObject(schema, value);
    }

    private static void validateObject(JsonObject schema, JsonObject value) {
        var required = schema.getJsonArray("required");
        if (required != null) {
            for (var r : required.getValuesAs(JsonString.class)) {
                if (!value.containsKey(r.getString())) {
                    throw new IllegalArgumentException("Missing required field: " + r.getString());
                }
            }
        }
        var props = schema.getJsonObject("properties");
        if (props == null) {
            return;
        }
        for (var e : props.entrySet()) {
            var name = e.getKey();
            if (!value.containsKey(name)) {
                continue;
            }
            var prop = e.getValue().asJsonObject();
            var v = value.get(name);
            validateProperty(name, prop, v);
        }
        for (var key : value.keySet()) {
            if (!props.containsKey(key)) {
                throw new IllegalArgumentException("Unexpected field: " + key);
            }
        }
    }

    private static void validateProperty(String name, JsonObject schema, JsonValue value) {
        var t = schema.getString("type", null);
        if (t == null) {
            return;
        }
        switch (t) {
            case "string" -> validateString(name, value, schema);
            case "number" -> validateNumber(name, value, schema);
            case "integer" -> validateInteger(name, value, schema);
            case "boolean" -> requireType(value, name, JsonValue.ValueType.TRUE, JsonValue.ValueType.FALSE);
            case "array" -> requireType(value, name, JsonValue.ValueType.ARRAY);
            case "object" -> {
                requireType(value, name, JsonValue.ValueType.OBJECT);
                validateObject(schema, value.asJsonObject());
            }
            default -> {
            }
        }
    }

    private static void validateString(String field, JsonValue v, JsonObject schema) {
        requireType(v, field, JsonValue.ValueType.STRING);
        var str = ((JsonString) v).getString();
        if (schema.containsKey("minLength") && str.length() < schema.getInt("minLength")) {
            throw new IllegalArgumentException("minLength violated for " + field);
        }
        if (schema.containsKey("maxLength") && str.length() > schema.getInt("maxLength")) {
            throw new IllegalArgumentException("maxLength violated for " + field);
        }
        if (schema.containsKey("enum")) {
            var match = schema.getJsonArray("enum")
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
        validateNumeric(field, v, schema, false);
    }

    private static void validateInteger(String field, JsonValue v, JsonObject schema) {
        validateNumeric(field, v, schema, true);
    }

    private static void validateNumeric(String field,
                                        JsonValue value,
                                        JsonObject schema,
                                        boolean integral) {
        requireType(value, field, JsonValue.ValueType.NUMBER);
        var num = (JsonNumber) value;
        if (integral && !num.isIntegral()) {
            throw new IllegalArgumentException("Expected integer for " + field);
        }
        if (schema.containsKey("minimum")) {
            var min = schema.getJsonNumber("minimum");
            if (integral ? num.longValue() < min.longValue() : num.doubleValue() < min.doubleValue()) {
                throw new IllegalArgumentException("minimum violated for " + field);
            }
        }
        if (schema.containsKey("maximum")) {
            var max = schema.getJsonNumber("maximum");
            if (integral ? num.longValue() > max.longValue() : num.doubleValue() > max.doubleValue()) {
                throw new IllegalArgumentException("maximum violated for " + field);
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

    private static void requireType(JsonValue value,
                                    String field,
                                    JsonValue.ValueType... allowed) {
        var vt = value.getValueType();
        for (var t : allowed) {
            if (vt == t) {
                return;
            }
        }
        var names = new StringBuilder();
        for (var i = 0; i < allowed.length; i++) {
            if (i > 0) {
                names.append(" or ");
            }
            names.append(allowed[i]);
        }
        throw new IllegalArgumentException("Expected " + names + " for " + field);
    }
}
