package com.amannmalik.mcp.server.logging;

public record SetLevelRequest(LoggingLevel level) {
    public SetLevelRequest {
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
    }
}
