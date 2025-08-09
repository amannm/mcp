package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.transport.AbstractEntityCodec;
import com.amannmalik.mcp.transport.JsonCodec;

public record PromptListChangedNotification() {
    public static final JsonCodec<PromptListChangedNotification> CODEC =
            AbstractEntityCodec.empty(PromptListChangedNotification::new);
}
