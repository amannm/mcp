package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;

import java.util.List;

public record PromptInstance(String description, List<PromptMessage> messages) {
    public PromptInstance {
        messages = SpiPreconditions.immutableList(messages);
    }

    @Override
    public List<PromptMessage> messages() {
        return SpiPreconditions.copyList(messages);
    }
}
