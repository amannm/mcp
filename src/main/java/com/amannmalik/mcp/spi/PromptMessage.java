package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.SpiPreconditions;

public record PromptMessage(Role role, PromptContent content) {
    public PromptMessage {
        SpiPreconditions.requireAllNonNull("role and content are required", role, content);
    }
}
