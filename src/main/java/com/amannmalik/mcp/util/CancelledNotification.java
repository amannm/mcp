package com.amannmalik.mcp.util;

import com.amannmalik.mcp.jsonrpc.RequestId;

/** Cancellation notification referencing a request. */
public record CancelledNotification(RequestId requestId, String reason) {
    public CancelledNotification {
        if (requestId == null) {
            throw new IllegalArgumentException("requestId is required");
        }
    }
}
