package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.PromptArgument;
import jakarta.json.*;

import java.util.Set;

public final class PromptArgumentAbstractEntityCodec extends AbstractEntityCodec<PromptArgument> {
    @Override
    public JsonObject toJson(PromptArgument a) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("name", a.name());
        if (a.title() != null) b.add("title", a.title());
        if (a.description() != null) b.add("description", a.description());
        if (a.required()) b.add("required", true);
        if (a._meta() != null) b.add("_meta", a._meta());
        return b.build();
    }

    @Override
    public PromptArgument fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("name", "title", "description", "required", "_meta"));
        String name = requireString(obj, "name");
        String title = obj.getString("title", null);
        String description = obj.getString("description", null);
        boolean required = obj.getBoolean("required", false);
        JsonObject meta = obj.getJsonObject("_meta");
        return new PromptArgument(name, title, description, required, meta);
    }
}
