package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.prompts.Role;

public record CreateMessageResponse(
        Role role,
        MessageContent content,
        String model,
        String stopReason
) {
    public CreateMessageResponse {
        if (role == null || content == null || model == null) {
            throw new IllegalArgumentException("role, content, and model are required");
        }
    }
}
