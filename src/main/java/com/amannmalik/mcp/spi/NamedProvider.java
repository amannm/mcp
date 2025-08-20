package com.amannmalik.mcp.spi;

import java.util.Optional;

public sealed interface NamedProvider<T extends DisplayNameProvider> extends Provider<T>
        permits PromptProvider, ResourceProvider, ToolProvider {
    default Optional<T> find(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name required");
        }
        Cursor cursor = Cursor.Start.INSTANCE;
        do {
            var page = list(cursor);
            for (var item : page.items()) {
                if (item.name().equals(name)) {
                    return Optional.of(item);
                }
            }
            cursor = page.nextCursor();
        } while (!(cursor instanceof Cursor.End));
        return Optional.empty();
    }
}
