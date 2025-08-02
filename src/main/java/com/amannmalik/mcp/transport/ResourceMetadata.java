package com.amannmalik.mcp.transport;

import java.util.List;

/**
 * Metadata for OAuth protected resources as defined in RFC9728.
 */
public record ResourceMetadata(String resource, List<String> authorizationServers) {
    public ResourceMetadata {
        if (resource == null || resource.isBlank()) {
            throw new IllegalArgumentException("resource required");
        }
        if (authorizationServers == null || authorizationServers.isEmpty()) {
            throw new IllegalArgumentException("authorizationServers required");
        }
        authorizationServers = List.copyOf(authorizationServers);
    }
}
