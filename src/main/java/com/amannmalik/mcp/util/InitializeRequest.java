package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.ClientInfo;
import com.amannmalik.mcp.core.Capabilities;
import com.amannmalik.mcp.core.ClientFeatures;

import java.util.Objects;

public record InitializeRequest(
        String protocolVersion,
        Capabilities capabilities,
        ClientInfo clientInfo,
        ClientFeatures features) {
    public InitializeRequest {
        protocolVersion = Objects.requireNonNull(protocolVersion, "protocolVersion");
        features = Objects.requireNonNullElse(features, ClientFeatures.EMPTY);
    }
}
