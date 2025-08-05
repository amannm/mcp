package com.amannmalik.mcp.logging;

import java.util.*;

public final class LogLevelManager {
    private LoggingLevel level;
    private final int limit;
    private int count;
    private boolean limited;
    private final List<LogEntry> messages = new ArrayList<>();

    public LogLevelManager(LoggingLevel level, int limit) {
        this.level = Objects.requireNonNull(level);
        this.limit = limit;
    }

    public void setLevel(LoggingLevel level) {
        this.level = Objects.requireNonNull(level);
    }

    public LoggingLevel level() {
        return level;
    }

    public void log(LoggingLevel lvl, String logger, String message) {
        if (count >= limit) {
            limited = true;
            return;
        }
        if (lvl.ordinal() >= level.ordinal()) {
            messages.add(new LogEntry(lvl, logger, message));
        }
        count++;
    }

    public List<LogEntry> messages() {
        return List.copyOf(messages);
    }

    public boolean rateLimited() {
        return limited;
    }

    public void clear() {
        messages.clear();
        count = 0;
        limited = false;
    }

    public record LogEntry(LoggingLevel level, String logger, String message) {}
}
