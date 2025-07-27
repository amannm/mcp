package com.amannmalik.mcp.prompts;

/** A single message in a prompt. Only text content is currently supported. */
public record PromptMessage(Role role, String text) {
    public PromptMessage {
        if (role == null || text == null) {
            throw new IllegalArgumentException("role and text are required");
        }
    }
}
