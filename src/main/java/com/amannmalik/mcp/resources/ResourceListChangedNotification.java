package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;

public record ResourceListChangedNotification() {
    public static final JsonCodec<ResourceListChangedNotification> CODEC =
            AbstractEntityCodec.empty(ResourceListChangedNotification::new);
}
