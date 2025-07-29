package com.amannmalik.mcp.server.resources;

import java.io.IOException;
import java.util.List;

public interface ResourceProvider extends AutoCloseable {
    ResourceList list(String cursor) throws IOException;
    ResourceBlock read(String uri) throws IOException;
    ResourceTemplatePage listTemplates(String cursor) throws IOException;
    ResourceSubscription subscribe(String uri, ResourceListener listener) throws IOException;
    default ResourceListSubscription subscribeList(ResourceListListener listener) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    default void close() throws IOException {}
}
