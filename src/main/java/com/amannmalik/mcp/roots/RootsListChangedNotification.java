package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.transport.AbstractEntityCodec;
import com.amannmalik.mcp.transport.JsonCodec;

public record RootsListChangedNotification() {
    public static final JsonCodec<RootsListChangedNotification> CODEC =
            AbstractEntityCodec.empty(RootsListChangedNotification::new);
}
