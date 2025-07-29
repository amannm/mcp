package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record PromptArgument(
        String name,
        String title,
        String description,
        boolean required,
        JsonObject _meta
) {
    public PromptArgument {
        name = InputSanitizer.requireClean(name);
        title = title == null ? null : InputSanitizer.requireClean(title);
        description = description == null ? null : InputSanitizer.requireClean(description);
        MetaValidator.requireValid(_meta);
    }
}
