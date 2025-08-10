package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.CreateMessageResponseAbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.sampling.MessageContent;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record CreateMessageResponse(
        Role role,
        MessageContent content,
        String model,
        String stopReason,
        JsonObject _meta
) {
    static final JsonCodec<CreateMessageResponse> CODEC = new CreateMessageResponseAbstractEntityCodec();

    public CreateMessageResponse {
        if (role == null || content == null || model == null) {
            throw new IllegalArgumentException("role, content, and model are required");
        }
        ValidationUtil.requireMeta(_meta);
    }

}
