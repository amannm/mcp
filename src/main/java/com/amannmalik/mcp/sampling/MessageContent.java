package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.content.ContentBlock;
import com.amannmalik.mcp.content.ContentCodec;
import com.amannmalik.mcp.core.JsonCodec;
import jakarta.json.JsonObject;

public interface MessageContent {
    JsonCodec<MessageContent> JSON = new JsonCodec<>() {
        @Override
        public JsonObject toJson(MessageContent entity) {
            return ContentCodec.toJsonObject((ContentBlock) entity);
        }

        @Override
        public MessageContent fromJson(JsonObject json) {
            return (MessageContent) ContentCodec.toContentBlock(json);
        }
    };
}
