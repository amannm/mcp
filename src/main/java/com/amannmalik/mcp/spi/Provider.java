package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.Change;
import com.amannmalik.mcp.core.InMemoryProvider;
import com.amannmalik.mcp.util.ExecutingProvider;

import java.io.Closeable;
import java.util.function.Consumer;

public sealed interface Provider<T> extends AutoCloseable permits InMemoryProvider, PromptProvider, ResourceProvider, RootsProvider, ToolProvider, ExecutingProvider {
    Pagination.Page<T> list(String cursor);

    default Closeable subscribe(Consumer<Change> listener) {
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
