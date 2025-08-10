package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.LoggingLevel;
import com.amannmalik.mcp.api.LoggingMessageNotification;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import jakarta.json.*;

import java.util.Set;

public class LoggingMessageNotificationAbstractEntityCodec extends AbstractEntityCodec<LoggingMessageNotification> {
    @Override
    public JsonObject toJson(LoggingMessageNotification n) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("level", n.level().name().toLowerCase())
                .add("data", n.data());
        if (n.logger() != null) b.add("logger", n.logger());
        return b.build();
    }

    @Override
    public LoggingMessageNotification fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("level", "logger", "data"));
        String raw = requireString(obj, "level");
        LoggingLevel level = LoggingLevel.fromString(raw);
        JsonValue data = obj.get("data");
        if (data == null) throw new IllegalArgumentException("data required");
        String logger = obj.containsKey("logger") ? obj.getString("logger") : null;
        return new LoggingMessageNotification(level, logger, data);
    }
}
