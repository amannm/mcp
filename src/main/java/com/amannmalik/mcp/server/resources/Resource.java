package com.amannmalik.mcp.server.resources;

public record Resource(
        String uri,
        String name,
        String title,
        String description,
        String mimeType,
        Long size,
        ResourceAnnotations annotations
) {
    public Resource {
        if (uri == null || name == null) {
            throw new IllegalArgumentException("uri and name are required");
        }
    }
}
