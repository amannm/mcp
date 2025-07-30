package com.amannmalik.mcp.server.logging;

import jakarta.json.JsonValue;

/**
 * Notification of a log message from server to client.
 */
public record LoggingMessageNotification(LoggingLevel level, String logger, JsonValue data) {
    public LoggingMessageNotification {
        if (level == null || data == null) {
            throw new IllegalArgumentException("level and data are required");
        }
    }
}
