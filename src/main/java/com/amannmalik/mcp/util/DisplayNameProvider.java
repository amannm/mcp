package com.amannmalik.mcp.util;

/**
 * Simple interface for entities that may expose a title for display purposes.
 */
public interface DisplayNameProvider {
    String name();

    String title();

    default String displayName() {
        String title = title();
        return title == null || title.isBlank() ? name() : title;
    }
}
