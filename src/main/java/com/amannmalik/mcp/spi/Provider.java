package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.InMemoryProvider;

public sealed interface Provider<T> extends AutoCloseable permits InMemoryProvider, PromptProvider, ResourceProvider, RootsProvider, ToolProvider, ExecutingProvider {
    Pagination.Page<T> list(Cursor cursor);

    default AutoCloseable onListChanged(Runnable listener) {
        return () -> {
        };
    }

    default boolean supportsListChanged() {
        return false;
    }

    @Override
    default void close() {
    }
}
