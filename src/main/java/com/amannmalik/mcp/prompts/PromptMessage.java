package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import jakarta.json.*;

import java.util.Set;

public record PromptMessage(Role role, PromptContent content) {
    public static final JsonCodec<PromptMessage> CODEC = new JsonCodec<>() {
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
            AbstractEntityCodec.requireOnlyKeys(obj, Set.of("role", "content"));
            String raw = obj.getString("role", null);
            if (raw == null) throw new IllegalArgumentException("role required");
            Role role = Role.valueOf(raw.toUpperCase());
            JsonObject c = obj.getJsonObject("content");
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
