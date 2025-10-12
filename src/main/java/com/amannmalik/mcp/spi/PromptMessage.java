package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

public record PromptMessage(Role role, PromptContent content) {
    public PromptMessage {
        ValidationUtil.requireAllNonNull("role and content are required", role, content);
    }
}
