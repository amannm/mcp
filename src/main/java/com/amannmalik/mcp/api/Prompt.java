package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record Prompt(
        String name,
        String title,
        String description,
        List<PromptArgument> arguments,
        JsonObject _meta
) implements DisplayNameProvider {
    public Prompt {
        name = ValidationUtil.requireClean(name);
        arguments = arguments == null || arguments.isEmpty() ? List.of() : List.copyOf(arguments);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        ValidationUtil.requireMeta(_meta);
    }

}
