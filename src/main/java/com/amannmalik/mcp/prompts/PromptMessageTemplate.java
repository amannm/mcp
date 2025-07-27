package com.amannmalik.mcp.prompts;

/** Template for a single message in a prompt. */
public record PromptMessageTemplate(Role role, String template) {
    public PromptMessageTemplate {
        if (role == null || template == null) {
            throw new IllegalArgumentException("role and template are required");
        }
    }
}
