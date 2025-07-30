package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;
import com.amannmalik.mcp.util.DisplayNameProvider;

public record PromptArgument(
        String name,
        String title,
        String description,
        boolean required,
        JsonObject _meta
) implements DisplayNameProvider {
    public PromptArgument {
        name = InputSanitizer.requireClean(name);
        title = InputSanitizer.cleanNullable(title);
        description = InputSanitizer.cleanNullable(description);
        MetaValidator.requireValid(_meta);
    }

}
