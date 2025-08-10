package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.codec.AbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;

public record PromptListChangedNotification() {
    public static final JsonCodec<PromptListChangedNotification> CODEC =
            AbstractEntityCodec.empty(PromptListChangedNotification::new);
}
