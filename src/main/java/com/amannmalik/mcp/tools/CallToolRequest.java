package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.*;

import java.util.Set;

public record CallToolRequest(String name,
                              JsonObject arguments,
                              JsonObject _meta) {
    public static final JsonCodec<CallToolRequest> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(CallToolRequest req) {
            JsonObjectBuilder b = Json.createObjectBuilder()
                    .add("name", req.name())
                    .add("arguments", req.arguments());
            if (req._meta() != null) b.add("_meta", req._meta());
            return b.build();
        }

        @Override
        public CallToolRequest fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            requireOnlyKeys(obj, Set.of("name", "arguments", "_meta"));
            String name = requireString(obj, "name");
            JsonObject arguments = obj.getJsonObject("arguments");
            JsonObject meta = obj.getJsonObject("_meta");
            return new CallToolRequest(name, arguments, meta);
        }
    };

    public CallToolRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = InputSanitizer.requireClean(name);
        arguments = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
        MetaValidator.requireValid(_meta);
    }
}
