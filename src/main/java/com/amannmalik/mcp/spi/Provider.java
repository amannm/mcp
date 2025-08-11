package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.Change;
import com.amannmalik.mcp.core.InMemoryProvider;

import java.util.Optional;
import java.util.function.Consumer;

public sealed interface Provider<T> extends AutoCloseable permits InMemoryProvider, PromptProvider, ResourceProvider, RootsProvider, ExecutingProvider {
    Pagination.Page<T> list(Cursor cursor);

    default Optional<T> find(String name) {
        if (name == null) throw new IllegalArgumentException("name required");
        Cursor cursor = Cursor.Start.INSTANCE;
        do {
            Pagination.Page<T> page = list(cursor);
            for (T item : page.items()) {
                if (item instanceof DisplayNameProvider d && d.name().equals(name)) {
                    return Optional.of(item);
                }
            }
            cursor = page.nextCursor();
        } while (!(cursor instanceof Cursor.End));
        return Optional.empty();
    }

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
