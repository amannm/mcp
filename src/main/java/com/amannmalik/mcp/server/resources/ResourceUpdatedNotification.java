package com.amannmalik.mcp.server.resources;

/**
 * Notification data for {@code notifications/resources/updated}.
 * <p>
 * The {@code title} field is optional but allows servers to provide a hint
 * about the updated resource. Clients SHOULD display it when available.
 */
public record ResourceUpdatedNotification(String uri, String title) {
    public ResourceUpdatedNotification {
        if (uri == null) {
            throw new IllegalArgumentException("uri is required");
        }
    }
}
