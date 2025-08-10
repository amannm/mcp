package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.PromptArgumentAbstractEntityCodec;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record PromptArgument(
        String name,
        String title,
        String description,
        boolean required,
        JsonObject _meta
) implements DisplayNameProvider {
    public static final JsonCodec<PromptArgument> CODEC = new PromptArgumentAbstractEntityCodec();

    public PromptArgument {
        name = ValidationUtil.requireClean(name);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        ValidationUtil.requireMeta(_meta);
    }

}
