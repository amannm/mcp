package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.PromptArgument;
import jakarta.json.*;

import java.util.Set;

public final class PromptArgumentAbstractEntityCodec extends AbstractEntityCodec<PromptArgument> {
    @Override
    public JsonObject toJson(PromptArgument a) {
        var b = Json.createObjectBuilder().add("name", a.name());
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
        var name = requireString(obj, "name");
        var title = obj.getString("title", null);
        var description = obj.getString("description", null);
        var required = obj.getBoolean("required", false);
        var meta = obj.getJsonObject("_meta");
        return new PromptArgument(name, title, description, required, meta);
    }
}
