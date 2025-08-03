package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record Prompt(
        String name,
        String title,
        String description,
        List<PromptArgument> arguments,
        JsonObject _meta
) implements DisplayNameProvider {
    public static final JsonCodec<Prompt> CODEC = new JsonCodec<>() {
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
            AbstractEntityCodec.requireOnlyKeys(obj, Set.of("name", "title", "description", "arguments", "_meta"));
            String name = obj.getString("name", null);
            if (name == null) throw new IllegalArgumentException("name required");
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
    };
    public Prompt {
        name = InputSanitizer.requireClean(name);
        arguments = arguments == null || arguments.isEmpty() ? List.of() : List.copyOf(arguments);
        title = InputSanitizer.cleanNullable(title);
        description = InputSanitizer.cleanNullable(description);
        MetaValidator.requireValid(_meta);
    }

}
