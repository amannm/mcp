package com.amannmalik.mcp.server.resources;

public record ResourceUpdate(String uri, String title) {
    public ResourceUpdate {
        if (uri == null) {
            throw new IllegalArgumentException("uri is required");
        }
    }
}
