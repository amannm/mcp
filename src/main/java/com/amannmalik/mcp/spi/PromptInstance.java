package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;

import java.util.List;

public record PromptInstance(String description, List<PromptMessage> messages) {
    public PromptInstance {
        messages = Immutable.list(messages);
    }

    /// Return an immutable view to avoid exposing internal representation.
    @Override
    public List<PromptMessage> messages() {
        return List.copyOf(messages);
    }
}
