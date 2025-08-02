package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.util.ListChangeSubscription;
import com.amannmalik.mcp.util.Pagination;
import java.util.Optional;

public interface ResourceProvider extends AutoCloseable {
    Pagination.Page<Resource> list(String cursor);

    ResourceBlock read(String uri);

    default Optional<Resource> get(String uri) {
        return Optional.empty();
    }

    Pagination.Page<ResourceTemplate> listTemplates(String cursor);

    ResourceSubscription subscribe(String uri, ResourceListener listener);

    default ListChangeSubscription subscribeList(ResourceListListener listener) {
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
