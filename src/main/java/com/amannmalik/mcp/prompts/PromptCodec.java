package com.amannmalik.mcp.prompts;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import java.util.List;
import java.util.Map;

/** JSON utilities for prompt messages. */
public final class PromptCodec {
    private PromptCodec() {}

    public static JsonObject toJsonObject(Prompt prompt) {
        var builder = Json.createObjectBuilder()
                .add("name", prompt.name());
        if (prompt.title() != null) builder.add("title", prompt.title());
        if (prompt.description() != null) builder.add("description", prompt.description());
        if (!prompt.arguments().isEmpty()) {
            JsonArrayBuilder args = Json.createArrayBuilder();
            for (PromptArgument a : prompt.arguments()) {
                JsonObjectBuilder ab = Json.createObjectBuilder().add("name", a.name());
                if (a.title() != null) ab.add("title", a.title());
                if (a.description() != null) ab.add("description", a.description());
                if (a.required()) ab.add("required", true);
                args.add(ab.build());
            }
            builder.add("arguments", args.build());
        }
        return builder.build();
    }

    public static JsonObject toJsonObject(PromptInstance inst) {
        JsonArrayBuilder msgs = Json.createArrayBuilder();
        for (PromptMessage m : inst.messages()) {
            msgs.add(Json.createObjectBuilder()
                    .add("role", m.role().name().toLowerCase())
                    .add("content", Json.createObjectBuilder()
                            .add("type", "text")
                            .add("text", m.text())
                            .build())
                    .build());
        }
        JsonObjectBuilder obj = Json.createObjectBuilder().add("messages", msgs.build());
        if (inst.description() != null) obj.add("description", inst.description());
        return obj.build();
    }

    public static Map<String, String> toArguments(JsonObject obj) {
        if (obj == null) return Map.of();
        return obj.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getValueType() == jakarta.json.JsonValue.ValueType.STRING
                        ? ((jakarta.json.JsonString) e.getValue()).getString()
                        : e.getValue().toString()));
    }
}
