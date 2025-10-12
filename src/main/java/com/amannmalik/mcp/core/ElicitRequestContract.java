package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.ElicitSchemaValidator;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public final class ElicitRequestContract {
    private ElicitRequestContract() {
    }

    public static String sanitizeMessage(String message) {
        ValidationUtil.requireNonNull(message, "message and requestedSchema are required");
        return ValidationUtil.requireClean(message);
    }

    public static JsonObject sanitizeSchema(JsonObject schema) {
        ValidationUtil.requireNonNull(schema, "message and requestedSchema are required");
        ElicitSchemaValidator.requireElicitSchema(schema);
        return schema;
    }

    public static JsonObject requireMeta(JsonObject meta) {
        ValidationUtil.requireMeta(meta);
        return meta;
    }
}
