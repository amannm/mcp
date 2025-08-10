package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.ToolAnnotations;
import jakarta.json.*;

import java.util.Set;

public final class ToolAnnotationsAbstractEntityCodec extends AbstractEntityCodec<ToolAnnotations> {
    @Override
    public JsonObject toJson(ToolAnnotations ann) {
        JsonObjectBuilder b = Json.createObjectBuilder();
        if (ann.title() != null) b.add("title", ann.title());
        if (ann.readOnlyHint() != null) b.add("readOnlyHint", ann.readOnlyHint());
        if (ann.destructiveHint() != null) b.add("destructiveHint", ann.destructiveHint());
        if (ann.idempotentHint() != null) b.add("idempotentHint", ann.idempotentHint());
        if (ann.openWorldHint() != null) b.add("openWorldHint", ann.openWorldHint());
        return b.build();
    }

    @Override
    public ToolAnnotations fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("title", "readOnlyHint", "destructiveHint", "idempotentHint", "openWorldHint"));
        String title = obj.getString("title", null);
        Boolean readOnly = obj.containsKey("readOnlyHint") ? obj.getBoolean("readOnlyHint") : null;
        Boolean destructive = obj.containsKey("destructiveHint") ? obj.getBoolean("destructiveHint") : null;
        Boolean idempotent = obj.containsKey("idempotentHint") ? obj.getBoolean("idempotentHint") : null;
        Boolean openWorld = obj.containsKey("openWorldHint") ? obj.getBoolean("openWorldHint") : null;
        return new ToolAnnotations(title, readOnly, destructive, idempotent, openWorld);
    }
}
