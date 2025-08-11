package com.amannmalik.mcp.spi;

public record PromptMessage(Role role, PromptContent content) {
    public PromptMessage {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
