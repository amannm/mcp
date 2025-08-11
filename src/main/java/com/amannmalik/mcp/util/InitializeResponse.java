package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.ServerFeatures;
import com.amannmalik.mcp.api.ServerInfo;
import com.amannmalik.mcp.core.Capabilities;

public record InitializeResponse(
        String protocolVersion,
        Capabilities capabilities,
        ServerInfo serverInfo,
        String instructions,
        ServerFeatures features) {
}
