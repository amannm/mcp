package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.ToolAnnotations;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public final class ToolAnnotationsAbstractEntityCodec extends AbstractEntityCodec<ToolAnnotations> {
    @Override
    public JsonObject toJson(ToolAnnotations ann) {
        var b = Json.createObjectBuilder();
        if (ann.title() != null) {
            b.add("title", ann.title());
        }
        if (ann.readOnlyHint() != null) {
            b.add("readOnlyHint", ann.readOnlyHint());
        }
        if (ann.destructiveHint() != null) {
            b.add("destructiveHint", ann.destructiveHint());
        }
        if (ann.idempotentHint() != null) {
            b.add("idempotentHint", ann.idempotentHint());
        }
        if (ann.openWorldHint() != null) {
            b.add("openWorldHint", ann.openWorldHint());
        }
        return b.build();
    }

    @Override
    public ToolAnnotations fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        requireOnlyKeys(obj, Set.of("title", "readOnlyHint", "destructiveHint", "idempotentHint", "openWorldHint"));
        var title = obj.getString("title", null);
        var readOnly = findBoolean(obj, "readOnlyHint").orElse(null);
        var destructive = findBoolean(obj, "destructiveHint").orElse(null);
        var idempotent = findBoolean(obj, "idempotentHint").orElse(null);
        var openWorld = findBoolean(obj, "openWorldHint").orElse(null);
        return new ToolAnnotations(title, readOnly, destructive, idempotent, openWorld);
    }
}
