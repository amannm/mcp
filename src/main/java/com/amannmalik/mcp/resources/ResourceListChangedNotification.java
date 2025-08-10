package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.codec.AbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;

public record ResourceListChangedNotification() {
    public static final JsonCodec<ResourceListChangedNotification> CODEC =
            AbstractEntityCodec.empty(ResourceListChangedNotification::new);
}
