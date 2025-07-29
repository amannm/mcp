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

    /**
     * Whether {@link #subscribe(String, ResourceListener)} delivers notifications.
     */
    default boolean supportsSubscribe() {
        return false;
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
