package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.util.ListChangeSubscription;

import java.util.List;

public interface RootsProvider extends AutoCloseable {

    List<Root> list();

    ListChangeSubscription subscribe(RootsListener listener);

    @Override
    default void close() {
    }

    default boolean supportsListChanged() {
        return false;
    }
}
