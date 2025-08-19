package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.ElicitResult;
import com.amannmalik.mcp.spi.ElicitationAction;
import jakarta.json.*;

import java.util.Set;

public class ElicitResultJsonCodec implements JsonCodec<ElicitResult> {
    @Override
    public JsonObject toJson(ElicitResult r) {
        var b = Json.createObjectBuilder()
                .add("action", r.action().name().toLowerCase());
        if (r.content() != null) b.add("content", r.content());
        if (r._meta() != null) b.add("_meta", r._meta());
        return b.build();
    }

    @Override
    public ElicitResult fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("action required");
        AbstractEntityCodec.requireOnlyKeys(obj, Set.of("action", "content", "_meta"));
        var raw = obj.getString("action", null);
        if (raw == null) throw new IllegalArgumentException("action required");
        ElicitationAction action;
        try {
            action = ElicitationAction.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid action", e);
        }
        var c = obj.get("content");
        JsonObject content = null;
        if (c != null) {
            if (c.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("content must be object");
            }
            content = c.asJsonObject();
        }
        return new ElicitResult(action, content, obj.getJsonObject("_meta"));
    }
}
