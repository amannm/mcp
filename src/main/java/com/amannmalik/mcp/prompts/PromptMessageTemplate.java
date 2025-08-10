package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.api.PromptContent;
import com.amannmalik.mcp.api.Role;

public record PromptMessageTemplate(Role role, PromptContent content) {
    public PromptMessageTemplate {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
