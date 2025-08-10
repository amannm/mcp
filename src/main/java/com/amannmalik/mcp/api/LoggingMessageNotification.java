package com.amannmalik.mcp.api;

import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record LoggingMessageNotification(LoggingLevel level, String logger, JsonValue data) {
     static final JsonCodec<LoggingMessageNotification> CODEC = new AbstractEntityCodec<>() {
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
    };

    public LoggingMessageNotification {
        if (level == null || data == null) throw new IllegalArgumentException("level and data are required");
        logger = ValidationUtil.cleanNullable(logger);
    }
}
