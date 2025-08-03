package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.server.roots.validation.InputSanitizer;

public record CancelledNotification(RequestId requestId, String reason) {
    public CancelledNotification {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId is required");
        }
        reason = InputSanitizer.cleanNullable(reason);
    }
}
