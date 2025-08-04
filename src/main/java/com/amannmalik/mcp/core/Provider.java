package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.Change;
import com.amannmalik.mcp.util.ChangeListener;
import com.amannmalik.mcp.util.ChangeSubscription;
import com.amannmalik.mcp.util.Pagination;

public interface Provider<T> extends AutoCloseable {
    Pagination.Page<T> list(String cursor);

    default ChangeSubscription subscribe(ChangeListener<Change> listener) {
        return () -> {};
    }

    default boolean supportsListChanged() {
        return false;
    }

    @Override
    default void close() {
    }
}
