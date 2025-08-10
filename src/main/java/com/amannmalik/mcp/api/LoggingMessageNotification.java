package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.LoggingMessageNotificationAbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonValue;

public record LoggingMessageNotification(LoggingLevel level, String logger, JsonValue data) {
    static final JsonCodec<LoggingMessageNotification> CODEC = new LoggingMessageNotificationAbstractEntityCodec();

    public LoggingMessageNotification {
        if (level == null || data == null) throw new IllegalArgumentException("level and data are required");
        logger = ValidationUtil.cleanNullable(logger);
    }

}
