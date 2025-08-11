package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.Capabilities;

public record InitializeResponse(
        String protocolVersion,
        Capabilities capabilities,
        ServerInfo serverInfo,
        String instructions,
        ServerFeatures features
) {
}
