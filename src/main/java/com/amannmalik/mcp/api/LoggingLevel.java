package com.amannmalik.mcp.api;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum LoggingLevel {
    DEBUG,
    INFO,
    NOTICE,
    WARNING,
    ERROR,
    CRITICAL,
    ALERT,
    EMERGENCY;

    private static final Map<String, LoggingLevel> BY_NAME;

    static {
        BY_NAME = Arrays.stream(values())
                .collect(Collectors.toUnmodifiableMap(l -> l.name().toLowerCase(), l -> l));
    }

    public static LoggingLevel fromString(String raw) {
        if (raw == null) throw new IllegalArgumentException("level required");
        LoggingLevel level = BY_NAME.get(raw.toLowerCase());
        if (level == null) throw new IllegalArgumentException("invalid level");
        return level;
    }
}
