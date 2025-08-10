package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.api.JsonCodec;

public record ToolListChangedNotification() {
    public static final JsonCodec<ToolListChangedNotification> CODEC =
            AbstractEntityCodec.empty(ToolListChangedNotification::new);
}
