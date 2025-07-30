package com.amannmalik.mcp.lifecycle;

public record InitializeResponse(
        String protocolVersion,
        Capabilities capabilities,
        ServerInfo serverInfo,
        String instructions,
        ServerFeatures features
) {
}
