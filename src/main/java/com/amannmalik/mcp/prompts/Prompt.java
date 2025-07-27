package com.amannmalik.mcp.prompts;

import java.util.List;

/** Metadata for a prompt that can be listed by the server. */
public record Prompt(
        String name,
        String title,
        String description,
        List<PromptArgument> arguments
) {
    public Prompt {
        if (name == null) throw new IllegalArgumentException("name is required");
        arguments = arguments == null || arguments.isEmpty() ? List.of() : List.copyOf(arguments);
    }
}
