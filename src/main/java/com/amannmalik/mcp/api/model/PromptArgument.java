package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record PromptArgument(
        String name,
        String title,
        String description,
        boolean required,
        JsonObject _meta
) implements DisplayNameProvider {

    public PromptArgument {
        name = ValidationUtil.requireClean(name);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        ValidationUtil.requireMeta(_meta);
    }

}
