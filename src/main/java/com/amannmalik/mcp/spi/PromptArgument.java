package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.SpiPreconditions;
import jakarta.json.JsonObject;

public record PromptArgument(
        String name,
        String title,
        String description,
        boolean required,
        JsonObject _meta
) implements DisplayNameProvider {
    public PromptArgument {
        name = SpiPreconditions.requireClean(name);
        title = SpiPreconditions.cleanNullable(title);
        description = SpiPreconditions.cleanNullable(description);
        SpiPreconditions.requireMeta(_meta);
    }
}
