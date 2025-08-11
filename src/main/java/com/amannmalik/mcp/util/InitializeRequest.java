package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.ClientInfo;
import com.amannmalik.mcp.core.Capabilities;
import com.amannmalik.mcp.core.ClientFeatures;

public record InitializeRequest(
        String protocolVersion,
        Capabilities capabilities,
        ClientInfo clientInfo,
        ClientFeatures features) {
    public InitializeRequest {
        if (protocolVersion == null) {
            throw new IllegalArgumentException("protocolVersion must not be null");
        }
        if (features == null) features = ClientFeatures.EMPTY;
    }
}