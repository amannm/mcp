package com.amannmalik.mcp.validation;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.Set;

public final class ElicitationSchemaValidator {
    private ElicitationSchemaValidator() {
    }

    public static void requireValid(JsonObject schema) {
        if (schema == null) throw new IllegalArgumentException("schema required");
        if (!"object".equals(schema.getString("type", null))) {
            throw new IllegalArgumentException("schema.type must be 'object'");
        }
        for (String key : schema.keySet()) {
            if (!ALLOWED_SCHEMA_KEYS.contains(key)) {
                throw new IllegalArgumentException("unsupported key '" + key + "' in schema");
            }
        }
        JsonObject props = schema.getJsonObject("properties");
        if (props == null || props.isEmpty()) {
            throw new IllegalArgumentException("schema.properties required");
        }
        for (var entry : props.entrySet()) {
            String name = entry.getKey();
            InputSanitizer.requireClean(name);
            JsonObject prop = entry.getValue().asJsonObject();
            String type = prop.getString("type", null);
            if (type == null) {
                throw new IllegalArgumentException("type required for property " + name);
            }
            switch (type) {
                case "string" -> validateString(prop, name);
                case "number", "integer" -> validateNumber(prop, name, type);
                case "boolean" -> validateBoolean(prop, name);
                default -> throw new IllegalArgumentException("invalid type for property " + name);
            }
        }
        if (schema.containsKey("required")) {
            JsonArray req = schema.getJsonArray("required");
            for (JsonString r : req.getValuesAs(JsonString.class)) {
                if (!props.containsKey(r.getString())) {
                    throw new IllegalArgumentException("required property missing: " + r.getString());
                }
            }
        }
    }

    private static void validateString(JsonObject prop, String name) {
        boolean hasEnum = prop.containsKey("enum");
        requireAllowedKeys(prop, hasEnum ? ENUM_KEYS : STRING_KEYS, name);
        validateCommonFields(prop, name);
        if (prop.containsKey("minLength") && prop.getInt("minLength") < 0) {
            throw new IllegalArgumentException("minLength must be >= 0 for " + name);
        }
        if (prop.containsKey("maxLength") && prop.getInt("maxLength") < 0) {
            throw new IllegalArgumentException("maxLength must be >= 0 for " + name);
        }
        if (hasEnum) {
            JsonArray vals = prop.getJsonArray("enum");
            if (vals.isEmpty()) {
                throw new IllegalArgumentException("enum must have values for " + name);
            }
            for (JsonValue v : vals) {
                if (v.getValueType() != JsonValue.ValueType.STRING) {
                    throw new IllegalArgumentException("enum values must be strings for " + name);
                }
            }
            if (prop.containsKey("enumNames")) {
                JsonArray names = prop.getJsonArray("enumNames");
                if (names.size() != vals.size()) {
                    throw new IllegalArgumentException("enumNames size mismatch for " + name);
                }
                for (JsonValue v : names) {
                    if (v.getValueType() != JsonValue.ValueType.STRING) {
                        throw new IllegalArgumentException("enumNames values must be strings for " + name);
                    }
                }
            }
        }
        if (prop.containsKey("format")) {
            switch (prop.getString("format")) {
                case "email", "uri", "date", "date-time" -> {
                }
                default -> throw new IllegalArgumentException("invalid format for " + name);
            }
        }
    }

    private static void validateNumber(JsonObject prop, String name, String type) {
        requireAllowedKeys(prop, NUMBER_KEYS, name);
        validateCommonFields(prop, name);
        if (prop.containsKey("minimum")) ensureNumber(prop.get("minimum"), name + ".minimum", type);
        if (prop.containsKey("maximum")) ensureNumber(prop.get("maximum"), name + ".maximum", type);
    }

    private static void validateBoolean(JsonObject prop, String name) {
        requireAllowedKeys(prop, BOOLEAN_KEYS, name);
        validateCommonFields(prop, name);
        if (prop.containsKey("default") && prop.get("default").getValueType() != JsonValue.ValueType.TRUE
                && prop.get("default").getValueType() != JsonValue.ValueType.FALSE) {
            throw new IllegalArgumentException("default must be boolean for " + name);
        }
    }

    private static void validateCommonFields(JsonObject prop, String name) {
        if (prop.containsKey("title")) InputSanitizer.requireClean(prop.getString("title"));
        if (prop.containsKey("description")) InputSanitizer.requireClean(prop.getString("description"));
    }

    private static void ensureNumber(JsonValue v, String field, String type) {
        if (v.getValueType() != JsonValue.ValueType.NUMBER) {
            throw new IllegalArgumentException(field + " must be a number");
        }
        if ("integer".equals(type) && !((JsonNumber) v).isIntegral()) {
            throw new IllegalArgumentException(field + " must be integral");
        }
    }

    private static final Set<String> STRING_KEYS = Set.of(
            "type", "title", "description", "minLength", "maxLength", "format"
    );

    private static final Set<String> ENUM_KEYS = Set.of(
            "type", "title", "description", "enum", "enumNames"
    );

    private static final Set<String> NUMBER_KEYS = Set.of(
            "type", "title", "description", "minimum", "maximum"
    );

    private static final Set<String> BOOLEAN_KEYS = Set.of(
            "type", "title", "description", "default"
    );
    private static final Set<String> ALLOWED_SCHEMA_KEYS = Set.of(
            "type", "properties", "required"
    );

    private static void requireAllowedKeys(JsonObject prop, Set<String> allowed, String name) {
        for (String key : prop.keySet()) {
            if (!allowed.contains(key)) {
                throw new IllegalArgumentException("unsupported key '" + key + "' in property " + name);
            }
        }
    }
}
