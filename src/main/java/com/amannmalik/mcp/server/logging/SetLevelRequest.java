package com.amannmalik.mcp.server.logging;

/** Request from client to set minimum log level. */
public record SetLevelRequest(LoggingLevel level) {
    public SetLevelRequest {
        if (level == null) {
            throw new IllegalArgumentException("level is required");
        }
    }
}
