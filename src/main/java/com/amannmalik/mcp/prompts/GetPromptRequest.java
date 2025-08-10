package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.*;

import java.util.*;

public record GetPromptRequest(String name,
                               Map<String, String> arguments,
                               JsonObject _meta) {
    public static final JsonCodec<GetPromptRequest> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(GetPromptRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder().add("name", req.name());
            if (!req.arguments().isEmpty()) {
                JsonObjectBuilder args = Json.createObjectBuilder();
                req.arguments().forEach(args::add);
                b.add("arguments", args.build());
            }
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public GetPromptRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("params required");
            requireOnlyKeys(obj, Set.of("name", "arguments", "_meta"));
            String name = requireString(obj, "name");
            JsonObject argsObj = obj.getJsonObject("arguments");
            Map<String, String> args = Map.of();
            if (argsObj != null) {
                Map<String, String> tmp = new HashMap<>();
                argsObj.forEach((k, v) -> {
                    if (v.getValueType() != JsonValue.ValueType.STRING) {
                        throw new IllegalArgumentException("argument values must be strings");
                    }
                    String key = ValidationUtil.requireClean(k);
                    String value = ValidationUtil.requireClean(((JsonString) v).getString());
                    tmp.put(key, value);
                });
                args = Map.copyOf(tmp);
            }
            JsonObject meta = obj.getJsonObject("_meta");
            return new GetPromptRequest(name, args, meta);
        }
    };

    public GetPromptRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = ValidationUtil.requireClean(name);
        arguments = ValidationUtil.requireCleanMap(arguments);
        ValidationUtil.requireMeta(_meta);
    }
}
