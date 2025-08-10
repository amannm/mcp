package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.InitializeResponseAbstractEntityCodec;
import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.core.Capabilities;

public record InitializeResponse(
        String protocolVersion,
        Capabilities capabilities,
        ServerInfo serverInfo,
        String instructions,
        ServerFeatures features
) {
    static final JsonCodec<InitializeResponse> CODEC = new InitializeResponseAbstractEntityCodec();

}
