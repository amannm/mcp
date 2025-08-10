package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.Prompt;
import com.amannmalik.mcp.api.PromptArgument;
import jakarta.json.*;

import java.util.*;

public final class PromptAbstractEntityCodec extends AbstractEntityCodec<Prompt> {
    @Override
    public JsonObject toJson(Prompt prompt) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("name", prompt.name());
        if (prompt.title() != null) b.add("title", prompt.title());
        if (prompt.description() != null) b.add("description", prompt.description());
        if (prompt._meta() != null) b.add("_meta", prompt._meta());
        if (!prompt.arguments().isEmpty()) {
            JsonArrayBuilder arr = Json.createArrayBuilder();
            prompt.arguments().forEach(a -> arr.add(PromptArgument.CODEC.toJson(a)));
            b.add("arguments", arr.build());
        }
        return b.build();
    }

    @Override
    public Prompt fromJson(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        requireOnlyKeys(obj, Set.of("name", "title", "description", "arguments", "_meta"));
        String name = requireString(obj, "name");
        String title = obj.getString("title", null);
        String description = obj.getString("description", null);
        JsonObject meta = obj.getJsonObject("_meta");
        JsonArray argsArr = obj.getJsonArray("arguments");
        List<PromptArgument> args = List.of();
        if (argsArr != null && !argsArr.isEmpty()) {
            List<PromptArgument> tmp = new ArrayList<>();
            for (JsonValue v : argsArr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("argument must be object");
                }
                tmp.add(PromptArgument.CODEC.fromJson(v.asJsonObject()));
            }
            args = List.copyOf(tmp);
        }
        return new Prompt(name, title, description, args, meta);
    }
}
