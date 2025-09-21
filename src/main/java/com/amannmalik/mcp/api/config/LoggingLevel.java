package com.amannmalik.mcp.api.config;

import java.util.Locale;

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
        if (raw == null) {
            throw new IllegalArgumentException("level required");
        }
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "debug" -> DEBUG;
            case "info" -> INFO;
            case "notice" -> NOTICE;
            case "warning" -> WARNING;
            case "error" -> ERROR;
            case "critical" -> CRITICAL;
            case "alert" -> ALERT;
            case "emergency" -> EMERGENCY;
            default -> throw new IllegalArgumentException("invalid level");
        };
    }
}
