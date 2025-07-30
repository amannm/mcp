package com.amannmalik.mcp.util;

import com.amannmalik.mcp.validation.InputSanitizer;

public record ProgressNotification(
        ProgressToken token,
        double progress,
        Double total,
        String message
) {
    public ProgressNotification {
        if (token == null) throw new IllegalArgumentException("token is required");
        if (Double.isNaN(progress) || Double.isInfinite(progress) || progress < 0.0) {
            throw new IllegalArgumentException("progress must be >= 0 and finite");
        }
        if (total != null) {
            if (Double.isNaN(total) || Double.isInfinite(total) || total <= 0.0) {
                throw new IllegalArgumentException("total must be > 0 and finite");
            }
            if (progress > total) {
                throw new IllegalArgumentException("progress must not exceed total");
            }
        }
        if (message != null) message = InputSanitizer.requireClean(message);
    }
}
