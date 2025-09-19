package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;

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
        ValidationUtil.requireNonNegative(progress, "progress");
        if (total != null) {
            total = ValidationUtil.requirePositive(total, "total");
            if (progress > total) {
                throw new IllegalArgumentException("progress must not exceed total");
            }
        }
        message = ValidationUtil.cleanNullable(message);
    }
}
