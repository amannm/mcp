package com.amannmalik.mcp.prompts;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import com.amannmalik.mcp.server.resources.ResourcesCodec;

import java.util.Base64;

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
                    .add("content", toJsonObject(m.content()))
                    .build());
        }
        JsonObjectBuilder obj = Json.createObjectBuilder().add("messages", msgs.build());
        if (inst.description() != null) obj.add("description", inst.description());
        return obj.build();
    }

    public static JsonObject toJsonObject(PromptPage page) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.prompts().forEach(p -> arr.add(toJsonObject(p)));
        JsonObjectBuilder builder = Json.createObjectBuilder().add("prompts", arr.build());
        if (page.nextCursor() != null) builder.add("nextCursor", page.nextCursor());
        return builder.build();
    }

    static JsonObject toJsonObject(PromptContent content) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("type", content.type());
        if (content.annotations() != null) {
            b.add("annotations", ResourcesCodec.toJsonObject(content.annotations()));
        }
        switch (content) {
            case PromptContent.Text t -> b.add("text", t.text());
            case PromptContent.Image i -> b.add("data", Base64.getEncoder().encodeToString(i.data()))
                    .add("mimeType", i.mimeType());
            case PromptContent.Audio a -> b.add("data", Base64.getEncoder().encodeToString(a.data()))
                    .add("mimeType", a.mimeType());
            case PromptContent.EmbeddedResource r -> b.add("resource", ResourcesCodec.toJsonObject(r.resource()));
            case PromptContent.ResourceLink l -> {
                for (var e : ResourcesCodec.toJsonObject(l.resource()).entrySet()) {
                    b.add(e.getKey(), e.getValue());
                }
            }
        }
        return b.build();
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
