package com.amannmalik.mcp.server.logging;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

/** JSON helpers for logging messages. */
public final class LoggingCodec {
    private LoggingCodec() {}

    public static JsonObject toJsonObject(LoggingNotification n) {
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("level", n.level().name().toLowerCase())
                .add("data", n.data());
        if (n.logger() != null) b.add("logger", n.logger());
        return b.build();
    }

    public static LoggingNotification toLoggingNotification(JsonObject obj) {
        LoggingLevel level = LoggingLevel.valueOf(obj.getString("level").toUpperCase());
        String logger = obj.getString("logger", null);
        return new LoggingNotification(level, logger, obj.get("data"));
    }

    public static JsonObject toJsonObject(SetLevelRequest req) {
        return Json.createObjectBuilder()
                .add("level", req.level().name().toLowerCase())
                .build();
    }

    public static SetLevelRequest toSetLevelRequest(JsonObject obj) {
        LoggingLevel level = LoggingLevel.valueOf(obj.getString("level").toUpperCase());
        return new SetLevelRequest(level);
    }
}
