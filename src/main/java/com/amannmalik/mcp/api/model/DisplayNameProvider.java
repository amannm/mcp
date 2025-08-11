package com.amannmalik.mcp.api.model;

public interface DisplayNameProvider {
    String name();

    String title();

    default String displayName() {
        String title = title();
        return title == null || title.isBlank() ? name() : title;
    }
}
