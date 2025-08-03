package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.core.AbstractEntityCodec;
import com.amannmalik.mcp.core.JsonCodec;

public record PingResponse() {
    public static final JsonCodec<PingResponse> CODEC =
            AbstractEntityCodec.empty(PingResponse::new);
}
