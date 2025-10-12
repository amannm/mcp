package com.amannmalik.mcp.spi.internal;

import com.amannmalik.mcp.util.ElicitSchemaValidator;
import jakarta.json.JsonObject;

public final class ElicitRequestContract {
    private ElicitRequestContract() {
    }

    public static String sanitizeMessage(String message) {
        SpiPreconditions.requireNonNull(message, "message and requestedSchema are required");
        return SpiPreconditions.requireClean(message);
    }

    public static JsonObject sanitizeSchema(JsonObject schema) {
        SpiPreconditions.requireNonNull(schema, "message and requestedSchema are required");
        ElicitSchemaValidator.requireElicitSchema(schema);
        return schema;
    }

    public static JsonObject requireMeta(JsonObject meta) {
        SpiPreconditions.requireMeta(meta);
        return meta;
    }
}
