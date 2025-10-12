package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record CreateMessageResponse(
        Role role,
        MessageContent content,
        String model,
        String stopReason,
        JsonObject _meta
) implements Result {
    public CreateMessageResponse {
        ValidationUtil.requireAllNonNull("role, content, and model are required", role, content, model);
        ValidationUtil.requireMeta(_meta);
    }
}
