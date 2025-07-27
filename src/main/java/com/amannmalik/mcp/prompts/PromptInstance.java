package com.amannmalik.mcp.prompts;

import java.util.List;

/** Result of resolving a prompt template. */
public record PromptInstance(String description, List<PromptMessage> messages) {
    public PromptInstance {
        messages = messages == null || messages.isEmpty() ? List.of() : List.copyOf(messages);
    }
}
