package com.amannmalik.mcp.codec;

import com.amannmalik.mcp.api.model.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import java.util.Set;

public final class PromptMessageAbstractEntityCodec extends AbstractEntityCodec<PromptMessage> {
    @Override
    public JsonObject toJson(PromptMessage m) {
        return Json.createObjectBuilder()
                .add("role", m.role().name().toLowerCase())
                .add("content", CONTENT_BLOCK_CODEC.toJson((ContentBlock) m.content()))
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
}
