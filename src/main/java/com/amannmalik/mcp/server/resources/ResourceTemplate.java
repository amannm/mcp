package com.amannmalik.mcp.server.resources;

public record ResourceTemplate(
        String uriTemplate,
        String name,
        String title,
        String description,
        String mimeType,
        ResourceAnnotations annotations
) {
    public ResourceTemplate {
        if (uriTemplate == null || name == null) {
            throw new IllegalArgumentException("uriTemplate and name are required");
        }
    }
}
