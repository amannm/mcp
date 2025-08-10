package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.codec.AbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;

public record RootsListChangedNotification() {
    public static final JsonCodec<RootsListChangedNotification> CODEC =
            AbstractEntityCodec.empty(RootsListChangedNotification::new);
}
