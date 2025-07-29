package com.amannmalik.mcp.server.logging;

public enum LoggingLevel {
    DEBUG,
    INFO,
    NOTICE,
    WARNING,
    ERROR,
    CRITICAL,
    ALERT,
    EMERGENCY;

    public static LoggingLevel fromString(String raw) {
        if (raw == null) throw new IllegalArgumentException("level required");
        try {
            return valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid level", e);
        }
    }
}
