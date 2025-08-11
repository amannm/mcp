package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.model.ServerFeatures;
import com.amannmalik.mcp.api.model.ServerInfo;
import com.amannmalik.mcp.core.Capabilities;

public record InitializeResponse(
        String protocolVersion,
        Capabilities capabilities,
        ServerInfo serverInfo,
        String instructions,
        ServerFeatures features) {
}
