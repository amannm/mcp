package com.amannmalik.mcp.api;

import com.amannmalik.mcp.core.Capabilities;
import com.amannmalik.mcp.core.ClientFeatures;

public record InitializeRequest(
        String protocolVersion,
        Capabilities capabilities,
        ClientInfo clientInfo,
        ClientFeatures features
) {

    public InitializeRequest {
        if (protocolVersion == null) {
            throw new IllegalArgumentException("protocolVersion must not be null");
        }
        if (features == null) features = ClientFeatures.EMPTY;
    }

}
