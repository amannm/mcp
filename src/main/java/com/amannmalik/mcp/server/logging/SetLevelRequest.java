package com.amannmalik.mcp.server.logging;

import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

public record SetLevelRequest(LoggingLevel level, JsonObject _meta) {
    public SetLevelRequest {
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
        MetaValidator.requireValid(_meta);
    }
}
