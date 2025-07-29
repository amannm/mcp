package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.InputSanitizer;


public record PromptArgument(
        String name,
        String title,
        String description,
        boolean required
) {
    public PromptArgument {
        name = InputSanitizer.requireClean(name);
        title = title == null ? null : InputSanitizer.requireClean(title);
        description = description == null ? null : InputSanitizer.requireClean(description);
    }
}
