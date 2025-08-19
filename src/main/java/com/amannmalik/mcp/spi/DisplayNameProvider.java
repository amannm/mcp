package com.amannmalik.mcp.spi;

public interface DisplayNameProvider {
    String name();

    String title();

    default String displayName() {
        var title = title();
        return title == null || title.isBlank() ? name() : title;
    }
}
