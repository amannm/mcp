package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.JsonUtil;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record GetPromptRequest(String name,
                               Map<String, String> arguments,
                               JsonObject _meta) {
    public static final JsonCodec<GetPromptRequest> CODEC = new JsonCodec<>() {
        private static Map<String, String> toArguments(JsonObject obj) {
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

        @Override
        public JsonObject toJson(GetPromptRequest req) {
            var builder = Json.createObjectBuilder().add("name", req.name());
            if (!req.arguments().isEmpty()) {
                JsonObjectBuilder args = Json.createObjectBuilder();
                req.arguments().forEach(args::add);
                builder.add("arguments", args.build());
            }
            if (req._meta() != null) builder.add("_meta", req._meta());
            return builder.build();
        }

        @Override
        public GetPromptRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("params required");
            JsonUtil.requireOnlyKeys(obj, Set.of("name", "arguments", "_meta"));
            String name = obj.getString("name", null);
            if (name == null) throw new IllegalArgumentException("name required");
            JsonObject argsObj = obj.getJsonObject("arguments");
            Map<String, String> args = toArguments(argsObj);
            JsonObject meta = obj.getJsonObject("_meta");
            return new GetPromptRequest(name, args, meta);
        }
    };

    public GetPromptRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = InputSanitizer.requireClean(name);
        arguments = InputSanitizer.requireCleanMap(arguments);
        MetaValidator.requireValid(_meta);
    }
}
