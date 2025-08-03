package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.*;

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
            String roleStr = obj.getString("role", null);
            if (roleStr == null) throw new IllegalArgumentException("role required");
            Role role = Role.valueOf(roleStr.toUpperCase());
            JsonObject contentObj = obj.getJsonObject("content");
            if (contentObj == null) throw new IllegalArgumentException("content required");
            ContentBlock block = ContentBlock.CODEC.fromJson(contentObj);
            if (!(block instanceof PromptContent pc)) {
                throw new IllegalArgumentException("content type unsupported");
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
