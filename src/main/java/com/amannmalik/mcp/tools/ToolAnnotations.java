package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.validation.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record ToolAnnotations(
        String title,
        Boolean readOnlyHint,
        Boolean destructiveHint,
        Boolean idempotentHint,
        Boolean openWorldHint
) {
    public static final JsonCodec<ToolAnnotations> CODEC = new AbstractEntityCodec<>() {
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
    };

    public ToolAnnotations {
        title = ValidationUtil.cleanNullable(title);
    }
}
