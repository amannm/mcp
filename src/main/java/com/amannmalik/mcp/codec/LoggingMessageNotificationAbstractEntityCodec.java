package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.LoggingLevel;
import com.amannmalik.mcp.api.LoggingMessageNotification;
import jakarta.json.*;

import java.util.Set;

public non-sealed class LoggingMessageNotificationAbstractEntityCodec extends AbstractEntityCodec<LoggingMessageNotification> {
    @Override
    public JsonObject toJson(LoggingMessageNotification n) {
        var b = Json.createObjectBuilder()
                .add("level", n.level().name().toLowerCase())
                .add("data", n.data());
        if (n.logger() != null) b.add("logger", n.logger());
        return b.build();
    }

    @Override
    public LoggingMessageNotification fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("level", "logger", "data"));
        var raw = requireString(obj, "level");
        var level = LoggingLevel.fromString(raw);
        var data = obj.get("data");
        if (data == null) throw new IllegalArgumentException("data required");
        var logger = obj.containsKey("logger") ? obj.getString("logger") : null;
        return new LoggingMessageNotification(level, logger, data);
    }
}
