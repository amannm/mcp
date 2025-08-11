package com.amannmalik.mcp.core;

import com.amannmalik.mcp.codec.JsonCodec;
import com.amannmalik.mcp.codec.ResourceMetadataJsonCodec;

import java.util.List;

public record ResourceMetadata(String resource, List<String> authorizationServers) {
    public static final JsonCodec<ResourceMetadata> CODEC = new ResourceMetadataJsonCodec();

    public ResourceMetadata {
        if (resource == null || resource.isBlank()) throw new IllegalArgumentException("resource required");
        if (authorizationServers == null || authorizationServers.isEmpty()) {
            throw new IllegalArgumentException("authorizationServers required");
        }
        authorizationServers = List.copyOf(authorizationServers);
    }

}
