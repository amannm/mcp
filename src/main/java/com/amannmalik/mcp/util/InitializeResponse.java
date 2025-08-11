package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.ServerFeature;
import com.amannmalik.mcp.api.ServerInfo;
import com.amannmalik.mcp.core.Capabilities;

import java.util.EnumSet;
import java.util.Set;

public record InitializeResponse(
        String protocolVersion,
        Capabilities capabilities,
        ServerInfo serverInfo,
        String instructions,
        Set<ServerFeature> features) {
    public InitializeResponse {
        features = features == null || features.isEmpty() ? Set.of() : EnumSet.copyOf(features);
    }
}
