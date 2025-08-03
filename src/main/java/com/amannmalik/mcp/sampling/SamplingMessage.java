package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;
import com.amannmalik.mcp.prompts.Role;
import jakarta.json.JsonObject;

public record SamplingMessage(Role role, MessageContent content) {
    public static final JsonCodec<SamplingMessage> JSON = new Codec();

    public SamplingMessage {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }

    private static final class Codec extends AbstractEntityCodec<SamplingMessage> {
        @Override
        public JsonObject toJson(SamplingMessage entity) {
            return object()
                    .add("role", entity.role().name().toLowerCase())
                    .add("content", MessageContent.JSON.toJson(entity.content()))
                    .build();
        }

        @Override
        public SamplingMessage fromJson(JsonObject json) {
            Role role = Role.valueOf(requireString(json, "role").toUpperCase());
            MessageContent content = MessageContent.JSON.fromJson(requireObject(json, "content"));
            return new SamplingMessage(role, content);
        }
    }
}
