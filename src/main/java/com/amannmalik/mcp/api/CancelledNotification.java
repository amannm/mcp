package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.CancelledNotificationJsonCodec;
import com.amannmalik.mcp.jsonrpc.RequestId;
import com.amannmalik.mcp.util.ValidationUtil;

public record CancelledNotification(RequestId requestId, String reason) {
    static final CancelledNotificationJsonCodec CODEC = new CancelledNotificationJsonCodec();

    public CancelledNotification {
        if (requestId == null) throw new IllegalArgumentException("requestId is required");
        reason = ValidationUtil.cleanNullable(reason);
    }

}
