package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.api.JsonCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.Set;

public record PromptArgument(
        String name,
        String title,
        String description,
        boolean required,
        JsonObject _meta
) implements DisplayNameProvider {
    public static final JsonCodec<PromptArgument> CODEC = new AbstractEntityCodec<>() {
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
    };

    public PromptArgument {
        name = ValidationUtil.requireClean(name);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        ValidationUtil.requireMeta(_meta);
    }

}
