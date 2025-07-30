package com.amannmalik.mcp.cli;

public record ServerConfig(TransportType transport,
                           int port,
                           String instructions,
                           String expectedAudience,
                           String resourceMetadataUrl) implements CliConfig {
    public ServerConfig {
        if (transport == null) throw new IllegalArgumentException("transport");
        if (transport == TransportType.HTTP && port <= 0) {
            throw new IllegalArgumentException("port required for HTTP");
        }
        if (resourceMetadataUrl != null && resourceMetadataUrl.isBlank()) {
            resourceMetadataUrl = null;
        }
    }
}
