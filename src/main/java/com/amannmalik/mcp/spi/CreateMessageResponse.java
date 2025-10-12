package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.SpiPreconditions;
import jakarta.json.JsonObject;

public record CreateMessageResponse(
        Role role,
        MessageContent content,
        String model,
        String stopReason,
        JsonObject _meta
) implements Result {
    public CreateMessageResponse {
        SpiPreconditions.requireAllNonNull("role, content, and model are required", role, content, model);
        SpiPreconditions.requireMeta(_meta);
    }
}
