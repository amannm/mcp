package com.amannmalik.mcp.server.logging;

import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record SetLevelRequest(LoggingLevel level, JsonObject _meta) {
    public SetLevelRequest {
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
        MetaValidator.requireValid(_meta);
    }
}
