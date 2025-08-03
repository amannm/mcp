package com.amannmalik.mcp.logging;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.JsonUtil;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.Set;

public record SetLevelRequest(LoggingLevel level, JsonObject _meta) {
    public static final JsonCodec<SetLevelRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(SetLevelRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("level", req.level().name().toLowerCase());
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public SetLevelRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonUtil.requireOnlyKeys(obj, Set.of("level", "_meta"));
            String raw = obj.getString("level", null);
            if (raw == null) throw new IllegalArgumentException("level required");
            LoggingLevel level = LoggingLevel.fromString(raw);
            return new SetLevelRequest(level, obj.getJsonObject("_meta"));
        }
    };

    public SetLevelRequest {
        if (level == null) throw new IllegalArgumentException("level is required");
        MetaValidator.requireValid(_meta);
    }
}
