package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.spi.internal.SpiPreconditions;
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
        name = SpiPreconditions.requireClean(name);
        arguments = SpiPreconditions.immutableList(arguments);
        title = SpiPreconditions.cleanNullable(title);
        description = SpiPreconditions.cleanNullable(description);
        SpiPreconditions.requireMeta(_meta);
    }

    @Override
    public List<PromptArgument> arguments() {
        return SpiPreconditions.copyList(arguments);
    }
}
