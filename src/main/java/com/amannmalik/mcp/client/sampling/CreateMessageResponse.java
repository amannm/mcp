package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.prompts.Role;

/** Model-generated message. */
public record CreateMessageResponse(
        Role role,
        MessageContent content,
        String model,
        String stopReason
) {
    public CreateMessageResponse {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
