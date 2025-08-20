package com.amannmalik.mcp.core;

import java.util.List;

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
