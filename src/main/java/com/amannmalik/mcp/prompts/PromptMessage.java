package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public record PromptMessage(Role role, PromptContent content) {
    public static final JsonCodec<PromptMessage> CODEC = new AbstractEntityCodec<>() {
        @Override
        public JsonObject toJson(PromptMessage m) {
            return Json.createObjectBuilder()
                    .add("role", m.role().name().toLowerCase())
                    .add("content", ContentBlock.CODEC.toJson((ContentBlock) m.content()))
                    .build();
        }

        @Override
        public PromptMessage fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            requireOnlyKeys(obj, Set.of("role", "content"));
            String raw = requireString(obj, "role");
            Role role = Role.valueOf(raw.toUpperCase());
            JsonObject c = getObject(obj, "content");
            if (c == null) throw new IllegalArgumentException("content required");
            PromptContent content = (PromptContent) ContentBlock.CODEC.fromJson(c);
            return new PromptMessage(role, content);
        }
    };

    public PromptMessage {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
