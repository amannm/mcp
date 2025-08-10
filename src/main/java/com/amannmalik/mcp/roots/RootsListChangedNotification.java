package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.jsonrpc.JsonCodec;

public record RootsListChangedNotification() {
    public static final JsonCodec<RootsListChangedNotification> CODEC =
            AbstractEntityCodec.empty(RootsListChangedNotification::new);
}
