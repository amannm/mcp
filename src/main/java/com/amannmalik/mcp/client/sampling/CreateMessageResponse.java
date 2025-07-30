package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.prompts.Role;

import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record CreateMessageResponse(
        Role role,
        MessageContent content,
        String model,
        String stopReason,
        JsonObject _meta
) {
    public CreateMessageResponse {
        if (role == null || content == null || model == null) {
            throw new IllegalArgumentException("role, content, and model are required");
        }
        MetaValidator.requireValid(_meta);
    }
}
