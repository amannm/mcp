package com.amannmalik.mcp.server.resources;

import java.util.List;

public interface ResourceProvider extends AutoCloseable {
    ResourceList list(String cursor);
    ResourceBlock read(String uri);
    ResourceTemplatePage listTemplates(String cursor);
    ResourceSubscription subscribe(String uri, ResourceListener listener);
    /**
     * Subscribe to changes in the list of resources.
     *
     * Implementations may return a no-op subscription when list change
     * notifications are not supported.
     */
    default ResourceListSubscription subscribeList(ResourceListListener listener) {
        return () -> { };
    }

    @Override
    default void close() {}
}
