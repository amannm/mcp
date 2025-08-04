package com.amannmalik.mcp.security;

import java.util.ArrayList;
import java.util.List;

public final class SecurityViolationLogger {
    private final List<Entry> entries = new ArrayList<>();

    public void log(String level, String message) {
        entries.add(new Entry(level, message));
    }

    public List<Entry> entries() {
        return List.copyOf(entries);
    }

    public record Entry(String level, String message) {}
}
