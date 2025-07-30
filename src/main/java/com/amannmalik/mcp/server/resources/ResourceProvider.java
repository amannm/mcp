package com.amannmalik.mcp.server.resources;

public interface ResourceProvider extends AutoCloseable {
    ResourceList list(String cursor);

    ResourceBlock read(String uri);

    ResourceTemplatePage listTemplates(String cursor);

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
