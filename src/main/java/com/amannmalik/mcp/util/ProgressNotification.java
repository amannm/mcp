package com.amannmalik.mcp.util;

/** Progress update notification. */
public record ProgressNotification(
        ProgressToken token,
        double progress,
        Double total,
        String message
) {
    public ProgressNotification {
        if (token == null) {
            throw new IllegalArgumentException("token is required");
        }
    }
}
