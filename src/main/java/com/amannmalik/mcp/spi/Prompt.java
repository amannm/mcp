package com.amannmalik.mcp.spi;

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
        arguments = ValidationUtil.immutableList(arguments);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        ValidationUtil.requireMeta(_meta);
    }

    @Override
    public List<PromptArgument> arguments() {
        return ValidationUtil.copyList(arguments);
    }
}
