package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.util.ContentBlock;
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
            Role role = requireRole(obj);
            ContentBlock block = requireContent(obj);
            if (!(block instanceof PromptContent pc)) {
                throw new IllegalArgumentException("content must be prompt-capable");
            }
            return new PromptMessage(role, pc);
        }
    };

    public PromptMessage {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
