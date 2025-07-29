package com.amannmalik.mcp.client.sampling;

import com.amannmalik.mcp.prompts.Role;

public record SamplingMessage(Role role, MessageContent content) {
    public SamplingMessage {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
