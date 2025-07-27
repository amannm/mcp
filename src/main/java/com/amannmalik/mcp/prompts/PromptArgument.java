package com.amannmalik.mcp.prompts;

/** Definition of a parameter that can be passed to a prompt template. */
public record PromptArgument(
        String name,
        String title,
        String description,
        boolean required
) {
    public PromptArgument {
        if (name == null) throw new IllegalArgumentException("name is required");
    }
}
