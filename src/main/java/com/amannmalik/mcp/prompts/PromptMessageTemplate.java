package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.api.model.PromptContent;
import com.amannmalik.mcp.api.model.Role;

public record PromptMessageTemplate(Role role, PromptContent content) {
    public PromptMessageTemplate {
        if (role == null || content == null) {
            throw new IllegalArgumentException("role and content are required");
        }
    }
}
