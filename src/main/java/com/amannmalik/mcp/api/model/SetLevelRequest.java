package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record SetLevelRequest(LoggingLevel level, JsonObject _meta) {
    public SetLevelRequest {
        if (level == null) throw new IllegalArgumentException("level is required");
        ValidationUtil.requireMeta(_meta);
    }
}
