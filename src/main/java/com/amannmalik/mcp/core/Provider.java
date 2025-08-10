package com.amannmalik.mcp.core;

import com.amannmalik.mcp.api.ChangeSubscription;
import com.amannmalik.mcp.api.Pagination;
import com.amannmalik.mcp.util.Change;

import java.util.function.Consumer;

public interface Provider<T> extends AutoCloseable {
    Pagination.Page<T> list(String cursor);

    default ChangeSubscription subscribe(Consumer<Change> listener) {
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
