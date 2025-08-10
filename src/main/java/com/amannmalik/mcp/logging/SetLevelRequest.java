package com.amannmalik.mcp.logging;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record SetLevelRequest(LoggingLevel level, JsonObject _meta) {
    public static final JsonCodec<SetLevelRequest> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(SetLevelRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("level", req.level().name().toLowerCase());
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public SetLevelRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            requireOnlyKeys(obj, Set.of("level", "_meta"));
            String raw = requireString(obj, "level");
            LoggingLevel level = LoggingLevel.fromString(raw);
            return new SetLevelRequest(level, obj.getJsonObject("_meta"));
        }
    };

    public SetLevelRequest {
        if (level == null) throw new IllegalArgumentException("level is required");
        ValidationUtil.requireMeta(_meta);
    }
}
