package com.amannmalik.mcp.server.logging;

import jakarta.json.JsonValue;

/** A log message notification sent from server to client. */
public record LoggingNotification(LoggingLevel level, String logger, JsonValue data) {
    public LoggingNotification {
        if (level == null || data == null) {
            throw new IllegalArgumentException("level and data are required");
        }
    }
}
