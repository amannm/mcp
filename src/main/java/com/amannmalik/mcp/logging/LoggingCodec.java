package com.amannmalik.mcp.logging;

import com.amannmalik.mcp.util.JsonUtil;
import jakarta.json.*;

import java.util.Set;

/// - [Logging](specification/2025-06-18/server/utilities/logging.mdx)
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
        JsonUtil.requireOnlyKeys(obj, Set.of("level", "logger", "data"));

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
        JsonObjectBuilder b = Json.createObjectBuilder()
                .add("level", req.level().name().toLowerCase());
        if (req._meta() != null) b.add("_meta", req._meta());
        return b.build();
    }

    public static SetLevelRequest toSetLevelRequest(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        JsonUtil.requireOnlyKeys(obj, Set.of("level", "_meta"));
        String raw;
        try {
            raw = obj.getString("level");
        } catch (Exception e) {
            throw new IllegalArgumentException("level required", e);
        }
        LoggingLevel level = LoggingLevel.fromString(raw);
        JsonObject meta = obj.getJsonObject("_meta");
        return new SetLevelRequest(level, meta);
    }
}
