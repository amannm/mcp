package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.server.roots.validation.InputSanitizer;
import jakarta.json.*;

import java.util.HashMap;
import java.util.Map;

public final class CompletionCodec {
    private CompletionCodec() {
    }

    public static JsonObject toJsonObject(CompleteRequest req) {
        JsonObjectBuilder obj = Json.createObjectBuilder()
                .add("ref", toJsonObject(req.ref()))
                .add("argument", Json.createObjectBuilder()
                        .add("name", req.argument().name())
                        .add("value", req.argument().value())
                        .build());
        if (req.context() != null && !req.context().arguments().isEmpty()) {
            JsonObjectBuilder ctx = Json.createObjectBuilder();
            req.context().arguments().forEach(ctx::add);
            obj.add("context", Json.createObjectBuilder().add("arguments", ctx.build()).build());
        }
        return obj.build();
    }

    public static CompleteRequest toCompleteRequest(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("request required");
        JsonObject refObj = obj.getJsonObject("ref");
        JsonObject arg = obj.getJsonObject("argument");
        if (refObj == null || arg == null) {
            throw new IllegalArgumentException("ref and argument required");
        }
        CompleteRequest.Ref ref = toRef(refObj);
        String name = arg.getString("name", null);
        String value = arg.getString("value", null);
        if (name == null || value == null) {
            throw new IllegalArgumentException("argument name and value required");
        }
        CompleteRequest.Argument argument = new CompleteRequest.Argument(name, value);
        CompleteRequest.Context ctx = null;
        if (obj.containsKey("context")) {
            JsonObject argsObj = obj.getJsonObject("context").getJsonObject("arguments");
            Map<String, String> args = new HashMap<>();
            if (argsObj != null) {
                argsObj.forEach((k, v) -> {
                    if (v.getValueType() != JsonValue.ValueType.STRING) {
                        throw new IllegalArgumentException("context arguments must be strings");
                    }
                    args.put(k, ((JsonString) v).getString());
                });
            }
            ctx = new CompleteRequest.Context(InputSanitizer.requireCleanMap(args));
        }
        return new CompleteRequest(ref, argument, ctx, null);
    }

    public static JsonObject toJsonObject(CompleteResult result) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        result.completion().values().forEach(arr::add);
        JsonObjectBuilder comp = Json.createObjectBuilder().add("values", arr.build());
        if (result.completion().total() != null) comp.add("total", result.completion().total());
        if (result.completion().hasMore() != null) comp.add("hasMore", result.completion().hasMore());
        JsonObjectBuilder obj = Json.createObjectBuilder().add("completion", comp.build());
        if (result._meta() != null) obj.add("_meta", result._meta());
        return obj.build();
    }

    public static CompleteResult toCompleteResult(JsonObject obj) {
        JsonObject comp = obj.getJsonObject("completion");
        var values = comp.getJsonArray("values").getValuesAs(JsonString.class).stream()
                .map(JsonString::getString)
                .toList();
        Integer total = comp.containsKey("total") ? comp.getInt("total") : null;
        Boolean hasMore = comp.containsKey("hasMore") ? comp.getBoolean("hasMore") : null;
        JsonObject meta = obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null;
        return new CompleteResult(new CompleteResult.Completion(values, total, hasMore), meta);
    }

    static JsonObject toJsonObject(CompleteRequest.Ref ref) {
        return switch (ref) {
            case CompleteRequest.Ref.PromptRef p -> {
                var b = Json.createObjectBuilder()
                        .add("type", p.type())
                        .add("name", p.name());
                if (p.title() != null) b.add("title", p.title());
                if (p._meta() != null) b.add("_meta", p._meta());
                yield b.build();
            }
            case CompleteRequest.Ref.ResourceRef r -> Json.createObjectBuilder()
                    .add("type", r.type())
                    .add("uri", r.uri())
                    .build();
        };
    }

    static CompleteRequest.Ref toRef(JsonObject obj) {
        if (obj == null) throw new IllegalArgumentException("ref required");
        String type = obj.getString("type", null);
        if (type == null) throw new IllegalArgumentException("ref type required");
        return switch (type) {
            case "ref/prompt" -> new CompleteRequest.Ref.PromptRef(
                    obj.getString("name"),
                    obj.containsKey("title") ? obj.getString("title") : null,
                    obj.containsKey("_meta") ? obj.getJsonObject("_meta") : null
            );
            case "ref/resource" -> new CompleteRequest.Ref.ResourceRef(obj.getString("uri"));
            default -> throw new IllegalArgumentException("unknown ref type");
        };
    }
}
