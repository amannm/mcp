package com.amannmalik.mcp.spi.internal;

import com.amannmalik.mcp.spi.ToolAnnotations;
import jakarta.json.JsonObject;

public final class ToolContract {
    private ToolContract() {
    }

    public static JsonObject requireInputSchema(JsonObject inputSchema) {
        return SpiPreconditions.requireNonNull(inputSchema, "inputSchema is required");
    }

    public static ToolAnnotations normalizeAnnotations(ToolAnnotations annotations) {
        if (annotations == null) {
            return null;
        }
        if (annotations.title() == null
                && annotations.readOnlyHint() == null
                && annotations.destructiveHint() == null
                && annotations.idempotentHint() == null
                && annotations.openWorldHint() == null) {
            return null;
        }
        return annotations;
    }
}
