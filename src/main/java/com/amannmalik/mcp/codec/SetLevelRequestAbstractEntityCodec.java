package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.LoggingLevel;
import com.amannmalik.mcp.api.SetLevelRequest;
import jakarta.json.*;

import java.util.Set;

public final class SetLevelRequestAbstractEntityCodec extends AbstractEntityCodec<SetLevelRequest> {
    @Override
    public JsonObject toJson(SetLevelRequest req) {
        return addMeta(
                Json.createObjectBuilder().add("level", req.level().name().toLowerCase()),
                req._meta()
        ).build();
    }

    @Override
    public SetLevelRequest fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("level", "_meta"));
        String raw = requireString(obj, "level");
        LoggingLevel level = LoggingLevel.fromString(raw);
        return new SetLevelRequest(level, meta(obj));
    }
}
