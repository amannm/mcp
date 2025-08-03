package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.ArrayList;
import java.util.List;

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
            var builder = Json.createObjectBuilder().add("name", prompt.name());
            if (prompt.title() != null) builder.add("title", prompt.title());
            if (prompt.description() != null) builder.add("description", prompt.description());
            if (prompt._meta() != null) builder.add("_meta", prompt._meta());
            if (!prompt.arguments().isEmpty()) {
                JsonArrayBuilder arr = Json.createArrayBuilder();
                prompt.arguments().forEach(a -> arr.add(PromptArgument.CODEC.toJson(a)));
                builder.add("arguments", arr.build());
            }
            return builder.build();
        }

        @Override
        public Prompt fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            String name = obj.getString("name", null);
            if (name == null) throw new IllegalArgumentException("name required");
            String title = obj.getString("title", null);
            String description = obj.getString("description", null);
            JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
            List<PromptArgument> args = List.of();
            var arr = obj.getJsonArray("arguments");
            if (arr != null && !arr.isEmpty()) {
                List<PromptArgument> tmp = new ArrayList<>();
                for (JsonValue v : arr) {
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
