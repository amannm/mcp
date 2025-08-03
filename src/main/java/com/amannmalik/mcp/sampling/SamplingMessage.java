package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.content.ContentCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.prompts.Role;
import com.amannmalik.mcp.util.JsonUtil;
import jakarta.json.*;

import java.util.Set;

public record SamplingMessage(Role role, MessageContent content) {
    public static final JsonCodec<SamplingMessage> CODEC = new JsonCodec<>() {
        @Override
        public JsonObject toJson(SamplingMessage m) {
            return Json.createObjectBuilder()
                    .add("role", m.role().name().toLowerCase())
                    .add("content", ContentCodec.toJsonObject((ContentBlock) m.content()))
                    .build();
        }

        @Override
        public SamplingMessage fromJson(JsonObject obj) {
            if (obj == null) throw new IllegalArgumentException("object required");
            JsonUtil.requireOnlyKeys(obj, Set.of("role", "content"));
            String raw = obj.getString("role", null);
            if (raw == null) throw new IllegalArgumentException("role required");
            Role role = Role.valueOf(raw.toUpperCase());
            JsonObject c = obj.getJsonObject("content");
            if (c == null) throw new IllegalArgumentException("content required");
            MessageContent content = (MessageContent) ContentCodec.toContentBlock(c);
            return new SamplingMessage(role, content);
        }
    };

    public SamplingMessage {
        if (role == null || content == null) throw new IllegalArgumentException("role and content are required");
    }
}
