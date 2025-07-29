package com.amannmalik.mcp.server.resources;

public interface ResourceProvider extends AutoCloseable {
    ResourceList list(String cursor);

    ResourceBlock read(String uri);

    ResourceTemplatePage listTemplates(String cursor);

    ResourceSubscription subscribe(String uri, ResourceListener listener);

    /**
     * Whether {@link #subscribe(String, ResourceListener)} is supported.
     */
    default boolean supportsSubscribe() {
        return false;
    }

    default ResourceListSubscription subscribeList(ResourceListListener listener) {
        return () -> {
        };
    }

    /**
     * Whether {@link #subscribeList(ResourceListListener)} delivers notifications.
     */
    default boolean supportsListChanged() {
        return false;
    }

    @Override
    default void close() {
    }
}
