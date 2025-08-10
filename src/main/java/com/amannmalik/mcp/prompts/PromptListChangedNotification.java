package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.api.JsonCodec;

public record PromptListChangedNotification() {
    public static final JsonCodec<PromptListChangedNotification> CODEC =
            AbstractEntityCodec.empty(PromptListChangedNotification::new);
}
