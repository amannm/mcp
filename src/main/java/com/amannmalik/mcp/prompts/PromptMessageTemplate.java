package com.amannmalik.mcp.prompts;

/** Template for a single message in a prompt. */
public record PromptMessageTemplate(Role role, PromptContent content) {
    public PromptMessageTemplate {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
