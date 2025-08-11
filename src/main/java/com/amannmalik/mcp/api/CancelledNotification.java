package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;

public record CancelledNotification(RequestId requestId, String reason) {
    public CancelledNotification {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        reason = ValidationUtil.cleanNullable(reason);
    }
}
