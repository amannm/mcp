package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.spi.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public final class SamplingMessageAbstractEntityCodec extends AbstractEntityCodec<SamplingMessage> {
    @Override
    public JsonObject toJson(SamplingMessage m) {
        return Json.createObjectBuilder()
                .add("role", m.role().name().toLowerCase())
                .add("content", CONTENT_BLOCK_CODEC.toJson((ContentBlock) m.content()))
                .build();
    }

    @Override
    public SamplingMessage fromJson(JsonObject obj) {
        if (obj == null) {
            throw new IllegalArgumentException("object required");
        }
        requireOnlyKeys(obj, Set.of("role", "content"));
        var role = requireRole(obj);
        var block = requireContent(obj);
        if (!(block instanceof MessageContent mc)) {
            throw new IllegalArgumentException("content must be message-capable");
        }
        return new SamplingMessage(role, mc);
    }
}
