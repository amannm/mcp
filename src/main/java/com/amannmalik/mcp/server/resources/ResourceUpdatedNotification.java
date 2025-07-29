package com.amannmalik.mcp.server.resources;

public record ResourceUpdatedNotification(String uri) {
    public ResourceUpdatedNotification {
        if (uri == null) {
            throw new IllegalArgumentException("uri is required");
        }
    }
}
