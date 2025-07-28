package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.InputSanitizer;

import java.util.List;

/** Metadata for a prompt that can be listed by the server. */
public record Prompt(
        String name,
        String title,
        String description,
        List<PromptArgument> arguments
) {
    public Prompt {
        name = InputSanitizer.requireClean(name);
        arguments = arguments == null || arguments.isEmpty() ? List.of() : List.copyOf(arguments);
        title = title == null ? null : InputSanitizer.requireClean(title);
        description = description == null ? null : InputSanitizer.requireClean(description);
    }
}
