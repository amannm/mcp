package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.util.ValidationUtil;

public record CancelledNotification(RequestId requestId, String reason) {
    public CancelledNotification {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        reason = ValidationUtil.cleanNullable(reason);
    }
}
