package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.util.Pagination;

public interface ResourceProvider extends AutoCloseable {
    Pagination.Page<Resource> list(String cursor);

    ResourceBlock read(String uri);

    Pagination.Page<ResourceTemplate> listTemplates(String cursor);

    ResourceSubscription subscribe(String uri, ResourceListener listener);

    default ResourceListSubscription subscribeList(ResourceListListener listener) {
        return () -> {
        };
    }

    default boolean supportsSubscribe() {
        return false;
    }

    default boolean supportsListChanged() {
        return false;
    }

    @Override
    default void close() {
    }
}
