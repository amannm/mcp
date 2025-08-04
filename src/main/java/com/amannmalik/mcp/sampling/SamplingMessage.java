package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.prompts.Role;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public record SamplingMessage(Role role, MessageContent content) {
    public static final JsonCodec<SamplingMessage> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(SamplingMessage m) {
            return Json.createObjectBuilder()
                    .add("role", m.role().name().toLowerCase())
                    .add("content", ContentBlock.CODEC.toJson((ContentBlock) m.content()))
                    .build();
        }

        @Override
        public SamplingMessage fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            AbstractEntityCodec.requireOnlyKeys(obj, Set.of("role", "content"));
            String raw = obj.getString("role", null);
            if (raw == null) throw new IllegalArgumentException("role required");
            Role role = Role.valueOf(raw.toUpperCase());
            JsonObject c = obj.getJsonObject("content");
            if (c == null) throw new IllegalArgumentException("content required");
            MessageContent content = (MessageContent) ContentBlock.CODEC.fromJson(c);
            return new SamplingMessage(role, content);
        }
    };

    public SamplingMessage {
        if (role == null || content == null) throw new IllegalArgumentException("role and content are required");
    }
}
