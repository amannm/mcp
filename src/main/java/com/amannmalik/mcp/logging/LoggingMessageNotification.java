package com.amannmalik.mcp.logging;

import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.JsonValue;

public record LoggingMessageNotification(LoggingLevel level, String logger, JsonValue data) {
    public LoggingMessageNotification {
        if (level == null || data == null) {
            throw new IllegalArgumentException("level and data are required");
        }
        logger = InputSanitizer.cleanNullable(logger);
    }
}
