package com.amannmalik.mcp;

import com.amannmalik.mcp.util.ChangeListener;
import com.amannmalik.mcp.util.ChangeSubscription;
import com.amannmalik.mcp.util.Pagination;

public interface Provider<T> extends AutoCloseable {
    Pagination.Page<T> list(String cursor);

    default ChangeSubscription subscribeList(ChangeListener<Void> listener) {
        return () -> {};
    }

    default boolean supportsListChanged() {
        return false;
    }

    @Override
    default void close() {
    }
}

