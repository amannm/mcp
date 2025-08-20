package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.LoggingLevel;
import com.amannmalik.mcp.api.SetLevelRequest;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public final class SetLevelRequestAbstractEntityCodec extends AbstractEntityCodec<SetLevelRequest> {
    @Override
    public JsonObject toJson(SetLevelRequest req) {
        var b = Json.createObjectBuilder().add("level", req.level().name().toLowerCase());
        if (req._meta() != null) {
            b.add("_meta", req._meta());
        }
        return b.build();
    }

    @Override
    public SetLevelRequest fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        requireOnlyKeys(obj, Set.of("level", "_meta"));
        var raw = requireString(obj, "level");
        var level = LoggingLevel.fromString(raw);
        return new SetLevelRequest(level, obj.getJsonObject("_meta"));
    }
}
