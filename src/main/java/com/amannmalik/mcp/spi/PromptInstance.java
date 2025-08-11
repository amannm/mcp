package com.amannmalik.mcp.spi;

import java.util.List;

public record PromptInstance(String description, List<PromptMessage> messages) {
    public PromptInstance {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }
}
