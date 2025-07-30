package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.annotations.AnnotationsCodec;
import com.amannmalik.mcp.server.resources.ResourcesCodec;
import com.amannmalik.mcp.util.EmptyJsonObjectCodec;
import com.amannmalik.mcp.util.PaginatedRequest;
import com.amannmalik.mcp.util.PaginatedResult;
import com.amannmalik.mcp.util.Pagination;
import com.amannmalik.mcp.util.PaginationCodec;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
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

    public static JsonObject toJsonObject(Pagination.Page<Prompt> page) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        page.items().forEach(p -> arr.add(toJsonObject(p)));
        JsonObjectBuilder builder = Json.createObjectBuilder().add("prompts", arr.build());
        PaginationCodec.toJsonObject(new PaginatedResult(page.nextCursor())).forEach(builder::add);
        return builder.build();
    }

    public static JsonObject toJsonObject(ListPromptsRequest req) {
        if (req == null) throw new IllegalArgumentException("request required");
        return PaginationCodec.toJsonObject(new PaginatedRequest(req.cursor()));
    }

    public static JsonObject toJsonObject(ListPromptsResult page) {
        return toJsonObject(new Pagination.Page<>(page.prompts(), page.nextCursor()));
    }

    public static JsonObject toJsonObject(PromptListChangedNotification n) {
        if (n == null) throw new IllegalArgumentException("notification required");
        return EmptyJsonObjectCodec.toJsonObject();
    }

    static JsonObject toJsonObject(PromptContent content) {
        JsonObjectBuilder b = Json.createObjectBuilder().add("type", content.type());
        if (content.annotations() != null) {
            b.add("annotations", AnnotationsCodec.toJsonObject(content.annotations()));
        }
        switch (content) {
            case PromptContent.Text t -> {
                if (content._meta() != null) b.add("_meta", content._meta());
                b.add("text", t.text());
            }
            case PromptContent.Image i -> {
                if (content._meta() != null) b.add("_meta", content._meta());
                b.add("data", Base64.getEncoder().encodeToString(i.data()))
                        .add("mimeType", i.mimeType());
            }
            case PromptContent.Audio a -> {
                if (content._meta() != null) b.add("_meta", content._meta());
                b.add("data", Base64.getEncoder().encodeToString(a.data()))
                        .add("mimeType", a.mimeType());
            }
            case PromptContent.EmbeddedResource r -> {
                if (content._meta() != null) b.add("_meta", content._meta());
                b.add("resource", ResourcesCodec.toJsonObject(r.resource()));
                if (r.annotations() != null) {
                    b.add("annotations", AnnotationsCodec.toJsonObject(r.annotations()));
                }
            }
            case PromptContent.ResourceLink l -> {
                JsonObject obj = ResourcesCodec.toJsonObject(l.resource());
                obj.forEach((k, v) -> {
                    if (!"_meta".equals(k)) b.add(k, v);
                });
                if (l.resource()._meta() != null) b.add("_meta", l.resource()._meta());
            }
        }
        return b.build();
    }

    public static GetPromptRequest toGetPromptRequest(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("params required");
        String name = obj.getString("name", null);
        if (name == null) throw new IllegalArgumentException("name required");
        JsonObject argsObj = obj.getJsonObject("arguments");
        Map<String, String> args = toArguments(argsObj);
        return new GetPromptRequest(name, args);
    }

    public static Map<String, String> toArguments(JsonObject obj) {
        if (obj == null) return Map.of();
        Map<String, String> args = new HashMap<>();
        obj.forEach((k, v) -> {
            if (v.getValueType() != JsonValue.ValueType.STRING) {
                throw new IllegalArgumentException("argument values must be strings");
            }
            String key = InputSanitizer.requireClean(k);
            String value = InputSanitizer.requireClean(((JsonString) v).getString());
            args.put(key, value);
        });
        return Map.copyOf(args);
    }

    public static Prompt toPrompt(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String name = obj.getString("name", null);
        if (name == null) throw new IllegalArgumentException("name required");
        String title = obj.getString("title", null);
        String description = obj.getString("description", null);
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        var argsArr = obj.getJsonArray("arguments");
        List<PromptArgument> args = List.of();
        if (argsArr != null && !argsArr.isEmpty()) {
            List<PromptArgument> tmp = new ArrayList<>();
            for (JsonValue v : argsArr) {
                if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                    throw new IllegalArgumentException("argument must be object");
                }
                tmp.add(toPromptArgument(v.asJsonObject()));
            }
            args = List.copyOf(tmp);
        }
        return new Prompt(name, title, description, args, meta);
    }

    public static PromptInstance toPromptInstance(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        String description = obj.getString("description", null);
        JsonArray arr = obj.getJsonArray("messages");
        if (arr == null) throw new IllegalArgumentException("messages required");
        List<PromptMessage> msgs = new ArrayList<>();
        for (JsonValue v : arr) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("message must be object");
            }
            msgs.add(toPromptMessage(v.asJsonObject()));
        }
        return new PromptInstance(description, msgs);
    }

    public static Pagination.Page<Prompt> toPromptPage(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("object required");
        JsonArray arr = obj.getJsonArray("prompts");
        if (arr == null) throw new IllegalArgumentException("prompts required");
        List<Prompt> prompts = new ArrayList<>();
        for (JsonValue v : arr) {
            if (v.getValueType() != JsonValue.ValueType.OBJECT) {
                throw new IllegalArgumentException("prompt must be object");
            }
            prompts.add(toPrompt(v.asJsonObject()));
        }
        String cursor = PaginationCodec.toPaginatedResult(obj).nextCursor();
        return new Pagination.Page<>(prompts, cursor);
    }

    public static ListPromptsResult toListPromptsResult(JsonObject obj) {
        Pagination.Page<Prompt> page = toPromptPage(obj);
        return new ListPromptsResult(page.items(), page.nextCursor());
    }

    public static ListPromptsRequest toListPromptsRequest(JsonObject obj) {
        String cursor = PaginationCodec.toPaginatedRequest(obj).cursor();
        return new ListPromptsRequest(cursor);
    }

    private static PromptArgument toPromptArgument(JsonObject obj) {
        String name = obj.getString("name", null);
        if (name == null) throw new IllegalArgumentException("name required");
        String title = obj.getString("title", null);
        String description = obj.getString("description", null);
        boolean required = obj.getBoolean("required", false);
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new PromptArgument(name, title, description, required, meta);
    }

    private static PromptMessage toPromptMessage(JsonObject obj) {
        String roleStr = obj.getString("role", null);
        if (roleStr == null) throw new IllegalArgumentException("role required");
        Role role = Role.valueOf(roleStr.toUpperCase());
        JsonObject contentObj = obj.getJsonObject("content");
        if (contentObj == null) throw new IllegalArgumentException("content required");
        return new PromptMessage(role, toPromptContent(contentObj));
    }

    private static PromptContent toPromptContent(JsonObject obj) {
        String type = obj.getString("type", null);
        if (type == null) throw new IllegalArgumentException("type required");
        var ann = obj.containsKey("annotations") ? AnnotationsCodec.toAnnotations(obj.getJsonObject("annotations")) : null;
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return switch (type) {
            case "text" -> new PromptContent.Text(obj.getString("text"), ann, meta);
            case "image" -> new PromptContent.Image(Base64.getDecoder().decode(obj.getString("data")), obj.getString("mimeType"), ann, meta);
            case "audio" -> new PromptContent.Audio(Base64.getDecoder().decode(obj.getString("data")), obj.getString("mimeType"), ann, meta);
            case "resource" -> new PromptContent.EmbeddedResource(ResourcesCodec.toResourceBlock(obj.getJsonObject("resource")), ann, meta);
            case "resource_link" -> new PromptContent.ResourceLink(ResourcesCodec.toResource(obj));
            default -> throw new IllegalArgumentException("unknown content type: " + type);
        };
    }
}
