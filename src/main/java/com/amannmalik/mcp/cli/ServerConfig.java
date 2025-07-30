package com.amannmalik.mcp.cli;

import java.util.List;

public record ServerConfig(
        TransportType transport,
        int port,
        String instructions,
        String expectedAudience,
        String resourceMetadataUrl,
        List<String> authorizationServers) implements CliConfig {
    public ServerConfig {
        if (transport == null) throw new IllegalArgumentException("transport");
        if (transport == TransportType.HTTP && port <= 0) {
            throw new IllegalArgumentException("port required for HTTP");
        }
      
        if (resourceMetadataUrl != null && !resourceMetadataUrl.isBlank()) {
            try {
                var uri = java.net.URI.create(resourceMetadataUrl);
                if (!uri.isAbsolute() || uri.getFragment() != null) {
                    throw new IllegalArgumentException("invalid resourceMetadataUrl");
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("invalid resourceMetadataUrl", e);
            }
        }

        authorizationServers = authorizationServers == null || authorizationServers.isEmpty()
                ? List.of()
                : List.copyOf(authorizationServers);
    }
}
