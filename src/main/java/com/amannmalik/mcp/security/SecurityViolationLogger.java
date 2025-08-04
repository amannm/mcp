package com.amannmalik.mcp.security;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SecurityViolationLogger {
    public enum Level { INFO, WARNING, ERROR }

    private final List<Entry> entries = new CopyOnWriteArrayList<>();

    public void log(Level level, String message) {
        entries.add(new Entry(level, message));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public record Entry(Level level, String message) {}
}
