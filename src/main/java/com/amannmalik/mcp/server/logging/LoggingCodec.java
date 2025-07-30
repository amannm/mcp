package com.amannmalik.mcp.server.logging;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

public final class LoggingCodec {
    private LoggingCodec() {
    }

    public static JsonObject toJsonObject(LoggingMessageNotification n) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("level", n.level().name().toLowerCase())
                .add("data", n.data());
        if (n.logger() != null) b.add("logger", n.logger());
        return b.build();
    }

    public static LoggingMessageNotification toLoggingMessageNotification(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");

        String rawLevel;
        try {
            rawLevel = obj.getString("level");
        } catch (Exception e) {
            throw new IllegalArgumentException("level required", e);
        }
        LoggingLevel level = LoggingLevel.fromString(rawLevel);

        JsonValue data = obj.get("data");
        if (data == null) throw new IllegalArgumentException("data required");

        String logger = null;
        if (obj.containsKey("logger")) {
            try {
                logger = obj.getString("logger");
            } catch (Exception e) {
                throw new IllegalArgumentException("logger must be a string", e);
            }
        }

        return new LoggingMessageNotification(level, logger, data);
    }

    public static JsonObject toJsonObject(SetLevelRequest req) {
        return Json.createObjectBuilder()
                .add("level", req.level().name().toLowerCase())
                .build();
    }

    public static SetLevelRequest toSetLevelRequest(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("level required");
        String raw;
        try {
            raw = obj.getString("level");
        } catch (Exception e) {
            throw new IllegalArgumentException("level required", e);
        }
        LoggingLevel level = LoggingLevel.fromString(raw);
        return new SetLevelRequest(level);
    }
}
