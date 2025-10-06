package com.amannmalik.mcp.spi;

public sealed interface Provider<T> extends AutoCloseable permits NamedProvider, RootsProvider, ExecutingProvider {

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
