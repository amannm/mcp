package com.amannmalik.mcp.util;

import com.amannmalik.mcp.api.config.LoggingLevel;

import java.lang.System.Logger;

public final class PlatformLog {
    private PlatformLog() {
    }

    public static Logger get(Class<?> cls) {
        return System.getLogger(cls.getName());
    }

    public static Logger.Level toPlatformLevel(LoggingLevel level) {
        if (level == null) {
            return Logger.Level.INFO;
        }
        return switch (level) {
            case DEBUG -> Logger.Level.DEBUG;
            case INFO, NOTICE -> Logger.Level.INFO;
            case WARNING -> Logger.Level.WARNING;
            default -> Logger.Level.ERROR;
        };
    }
}
