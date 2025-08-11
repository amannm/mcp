package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.api.MessageContent;

public record SamplingMessage(Role role, MessageContent content) {
    public SamplingMessage {
        if (role == null || content == null) throw new IllegalArgumentException("role and content are required");
    }
}
