package com.amannmalik.mcp.prompts;


public record PromptMessageTemplate(Role role, PromptContent content) {
    public PromptMessageTemplate {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
