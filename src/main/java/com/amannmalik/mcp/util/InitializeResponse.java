package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.ServerFeature;
import com.amannmalik.mcp.api.ServerInfo;
import com.amannmalik.mcp.core.Capabilities;

import java.util.Objects;
import java.util.Set;

public record InitializeResponse(
        String protocolVersion,
        Capabilities capabilities,
        ServerInfo serverInfo,
        String instructions,
        Set<ServerFeature> features) {
    public InitializeResponse {
        protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion");
        capabilities = Objects.requireNonNull(capabilities, "capabilities");
        serverInfo = Objects.requireNonNull(serverInfo, "serverInfo");
        instructions = Objects.requireNonNullElse(instructions, "");
        features = Immutable.enumSet(features);
    }

    @Override
    public Set<ServerFeature> features() {
        return Set.copyOf(features);
    }
}
