package com.amannmalik.mcp.server.logging;

import jakarta.json.JsonValue;

public record LoggingMessageNotification(LoggingLevel level, String logger, JsonValue data) {
    public LoggingMessageNotification {
        if (level == null || data == null) {
            throw new IllegalArgumentException("level and data are required");
        }
    }
}
