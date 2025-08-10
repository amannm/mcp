package com.amannmalik.mcp.api;

import com.amannmalik.mcp.jsonrpc.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.HashMap;
import java.util.Map;

public record CompleteRequest(
        Ref ref,
        Argument argument,
        Context context,
        JsonObject _meta
) {
    static final JsonCodec<CompleteRequest> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(CompleteRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("ref", Ref.CODEC.toJson(req.ref()))
                    .add("argument", Argument.CODEC.toJson(req.argument()));
            if (req.context() != null && !req.context().arguments().isEmpty()) {
                b.add("context", Context.CODEC.toJson(req.context()));
            }
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public CompleteRequest fromJson(JsonObject obj) {
            JsonObject refObj = obj.getJsonObject("ref");
            JsonObject argObj = obj.getJsonObject("argument");
            if (refObj == null || argObj == null) {
                throw new IllegalArgumentException("ref and argument required");
            }
            Ref ref = Ref.CODEC.fromJson(refObj);
            Argument arg = Argument.CODEC.fromJson(argObj);
            Context ctx = obj.containsKey("context") ? Context.CODEC.fromJson(obj.getJsonObject("context")) : null;
            JsonObject meta = obj.getJsonObject("_meta");
            return new CompleteRequest(ref, arg, ctx, meta);
        }
    };

    public CompleteRequest {
        if (ref == null || argument == null) {
            throw new IllegalArgumentException("ref and argument are required");
        }
        ValidationUtil.requireMeta(_meta);
    }

    public record Argument(String name, String value) {
        public static final JsonCodec<Argument> CODEC = new JsonCodec<>() {
            @Override
            public JsonObject toJson(Argument arg) {
                return Json.createObjectBuilder()
                        .add("name", arg.name())
                        .add("value", arg.value())
                        .build();
            }

            @Override
            public Argument fromJson(JsonObject obj) {
                String name = obj.getString("name", null);
                String value = obj.getString("value", null);
                if (name == null || value == null) {
                    throw new IllegalArgumentException("name and value required");
                }
                return new Argument(name, value);
            }
        };

        public Argument(String name, String value) {
            if (name == null || value == null) {
                throw new IllegalArgumentException("name and value are required");
            }
            this.name = ValidationUtil.requireClean(name);
            this.value = ValidationUtil.requireClean(value);
        }
    }

    public record Context(Map<String, String> arguments) {
        public static final JsonCodec<Context> CODEC = new JsonCodec<>() {
            @Override
            public JsonObject toJson(Context ctx) {
                JsonObjectBuilder args = Json.createObjectBuilder();
                ctx.arguments().forEach(args::add);
                return Json.createObjectBuilder().add("arguments", args.build()).build();
            }

            @Override
            public Context fromJson(JsonObject obj) {
                JsonObject argsObj = obj.getJsonObject("arguments");
                Map<String, String> map = new HashMap<>();
                if (argsObj != null) {
                    argsObj.forEach((k, v) -> {
                        if (v.getValueType() != JsonValue.ValueType.STRING) {
                            throw new IllegalArgumentException("context arguments must be strings");
                        }
                        map.put(k, ((JsonString) v).getString());
                    });
                }
                return new Context(ValidationUtil.requireCleanMap(map));
            }
        };

        public Context(Map<String, String> arguments) {
            this.arguments = ValidationUtil.requireCleanMap(arguments);
        }

        @Override
        public Map<String, String> arguments() {
            return Map.copyOf(arguments);
        }
    }

    public sealed interface Ref permits Ref.PromptRef, Ref.ResourceRef {
        JsonCodec<Ref> CODEC = new JsonCodec<>() {
            @Override
            public JsonObject toJson(Ref ref) {
                return switch (ref) {
                    case PromptRef p -> {
                        JsonObjectBuilder b = Json.createObjectBuilder()
                                .add("type", p.type())
                                .add("name", p.name());
                        if (p.title() != null) b.add("title", p.title());
                        if (p._meta() != null) b.add("_meta", p._meta());
                        yield b.build();
                    }
                    case ResourceRef r -> Json.createObjectBuilder()
                            .add("type", r.type())
                            .add("uri", r.uri())
                            .build();
                };
            }

            @Override
            public Ref fromJson(JsonObject obj) {
                String type = obj.getString("type", null);
                if (type == null) throw new IllegalArgumentException("type required");
                return switch (type) {
                    case "ref/prompt" -> new PromptRef(
                            obj.getString("name"),
                            obj.getString("title", null),
                            obj.getJsonObject("_meta")
                    );
                    case "ref/resource" -> new ResourceRef(obj.getString("uri"));
                    default -> throw new IllegalArgumentException("unknown ref type");
                };
            }
        };

        String type();

        record PromptRef(String name, String title, JsonObject _meta) implements Ref {
            public PromptRef(String name, String title, JsonObject _meta) {
                if (name == null) throw new IllegalArgumentException("name required");
                this.name = ValidationUtil.requireClean(name);
                this.title = ValidationUtil.cleanNullable(title);
                ValidationUtil.requireMeta(_meta);
                this._meta = _meta;
            }

            @Override
            public String type() {
                return "ref/prompt";
            }
        }

        record ResourceRef(String uri) implements Ref {
            public ResourceRef(String uri) {
                if (uri == null) throw new IllegalArgumentException("uri required");
                this.uri = ValidationUtil.requireAbsoluteTemplate(uri);
            }

            @Override
            public String type() {
                return "ref/resource";
            }
        }
    }
}
