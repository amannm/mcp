package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.util.PaginatedResult;
import com.amannmalik.mcp.util.PaginationCodec;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class PromptCodec {
    private PromptCodec() {
    }

    public static JsonObject toJsonObject(Prompt prompt) {
        var builder = Json.createObjectBuilder()
                .add("name", prompt.name());
        if (prompt.title() != null) builder.add("title", prompt.title());
        if (prompt.description() != null) builder.add("description", prompt.description());
        if (prompt._meta() != null) builder.add("_meta", prompt._meta());
        if (!prompt.arguments().isEmpty()) {
            JsonArrayBuilder args = Json.createArrayBuilder();
            for (PromptArgument a : prompt.arguments()) {
                JsonObjectBuilder ab = Json.createObjectBuilder().add("name", a.name());
                if (a.title() != null) ab.add("title", a.title());
                if (a.description() != null) ab.add("description", a.description());
                if (a.required()) ab.add("required", true);
                if (a._meta() != null) ab.add("_meta", a._meta());
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
        PaginationCodec.toJsonObject(new PaginatedResult(page.nextCursor())).forEach(builder::add);
        return builder.build();
    }

    public static JsonObject toJsonObject(PromptsListChangedNotification n) {
        if (n == null) throw new IllegalArgumentException("notification required");
        return Json.createObjectBuilder().build();
    }

    public static PromptsListChangedNotification toPromptsListChangedNotification(JsonObject obj) {
        if (obj != null && !obj.isEmpty()) {
            throw new IllegalArgumentException("unexpected fields");
        }
        return new PromptsListChangedNotification();
    }

    static JsonObject toJsonObject(PromptContent content) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("type", content.type());
        if (content.annotations() != null) {
            b.add("annotations", ResourcesCodec.toJsonObject(content.annotations()));
        }
        if (content._meta() != null) b.add("_meta", content._meta());
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
        Map<String, String> args = new HashMap<>();
        obj.forEach((k, v) -> {
            if (v.getValueType() != JsonValue.ValueType.STRING) {
                throw new IllegalArgumentException("argument values must be strings");
            }
            args.put(k, ((JsonString) v).getString());
        });
        return args;
    }
}
