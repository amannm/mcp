package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.Change;
import com.amannmalik.mcp.core.InMemoryProvider;

import java.util.function.Consumer;

public sealed interface Provider<T> extends AutoCloseable permits InMemoryProvider, PromptProvider, ResourceProvider, RootsProvider, ToolProvider, ExecutingProvider {
    Pagination.Page<T> list(Cursor cursor);

    default AutoCloseable subscribe(Consumer<Change> listener) {
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
