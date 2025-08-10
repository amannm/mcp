package com.amannmalik.mcp.api.sampling;

import com.amannmalik.mcp.api.*;
import com.amannmalik.mcp.sampling.MessageContent;
import com.amannmalik.mcp.core.AbstractEntityCodec;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public record SamplingMessage(Role role, MessageContent content) {
    static final JsonCodec<SamplingMessage> CODEC = new AbstractEntityCodec<>() {
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
            requireOnlyKeys(obj, Set.of("role", "content"));
            Role role = requireRole(obj);
            ContentBlock block = requireContent(obj);
            if (!(block instanceof MessageContent mc)) {
                throw new IllegalArgumentException("content must be message-capable");
            }
            return new SamplingMessage(role, mc);
        }
    };

    public SamplingMessage {
        if (role == null || content == null) throw new IllegalArgumentException("role and content are required");
    }
}
