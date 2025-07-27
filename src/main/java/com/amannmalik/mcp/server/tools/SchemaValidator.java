package com.amannmalik.mcp.server.tools;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;

/** Very small subset of JSON Schema validation used for tool inputs. */
public final class SchemaValidator {
    private SchemaValidator() {}

    public static void validate(JsonObject schema, JsonObject value) {
        if (schema == null) return;
        if ("object".equals(schema.getString("type", null))) {
            JsonArray required = schema.getJsonArray("required");
            if (required != null) {
                for (var r : required.getValuesAs(JsonString.class)) {
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
                        switch (t) {
                            case "string" -> {
                                if (!value.get(e.getKey()).getValueType().equals(jakarta.json.JsonValue.ValueType.STRING))
                                    throw new IllegalArgumentException("Expected string for " + e.getKey());
                            }
                            case "number" -> {
                                var vt = value.get(e.getKey()).getValueType();
                                if (vt != jakarta.json.JsonValue.ValueType.NUMBER)
                                    throw new IllegalArgumentException("Expected number for " + e.getKey());
                            }
                            case "object" -> validate(e.getValue().asJsonObject(), value.getJsonObject(e.getKey()));
                            default -> {}
                        }
                    }
                }
            }
        }
    }
}
