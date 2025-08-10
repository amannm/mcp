package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.codec.AbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;

public record ToolListChangedNotification() {
    public static final JsonCodec<ToolListChangedNotification> CODEC =
            AbstractEntityCodec.empty(ToolListChangedNotification::new);
}
