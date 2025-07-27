package com.amannmalik.mcp.lifecycle;

public record InitializeRequest(
        String protocolVersion,
        Capabilities capabilities,
        ClientInfo clientInfo
) {
    public InitializeRequest {
        if (protocolVersion == null) {
            throw new IllegalArgumentException("protocolVersion must not be null");
        }
    }
}
