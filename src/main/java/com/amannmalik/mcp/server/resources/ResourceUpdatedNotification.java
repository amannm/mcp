package com.amannmalik.mcp.server.resources;

public record ResourceUpdatedNotification(String uri, String title) {
    public ResourceUpdatedNotification {
        if (uri == null) {
            throw new IllegalArgumentException("uri is required");
        }
    }
}
