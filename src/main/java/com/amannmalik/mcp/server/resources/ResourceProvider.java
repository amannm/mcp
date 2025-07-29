package com.amannmalik.mcp.server.resources;

import java.util.List;

public interface ResourceProvider extends AutoCloseable {
    ResourceList list(String cursor);
    ResourceBlock read(String uri);
    ResourceTemplatePage listTemplates(String cursor);
    ResourceSubscription subscribe(String uri, ResourceListener listener);
    default ResourceListSubscription subscribeList(ResourceListListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    default void close() {}
}
