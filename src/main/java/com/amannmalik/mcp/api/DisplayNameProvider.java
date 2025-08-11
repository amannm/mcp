package com.amannmalik.mcp.api;

public interface DisplayNameProvider {
    String name();

    String title();

    default String displayName() {
        String title = title();
        return title == null || title.isBlank() ? name() : title;
    }
}
