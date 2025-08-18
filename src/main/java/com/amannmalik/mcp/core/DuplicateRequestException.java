package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.RequestId;

public final class DuplicateRequestException extends IllegalArgumentException {
    public DuplicateRequestException(RequestId id) {
        super("Duplicate request: " + id);
    }
}
