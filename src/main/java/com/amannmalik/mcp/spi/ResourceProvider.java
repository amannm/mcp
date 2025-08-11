package com.amannmalik.mcp.spi;

import java.io.Closeable;
import java.util.Optional;
import java.util.function.Consumer;

/// - [Resources](specification/2025-06-18/server/resources.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
public non-sealed interface ResourceProvider extends Provider<Resource> {
    ResourceBlock read(String uri);

    default Optional<Resource> get(String uri) {
        return Optional.empty();
    }

    Pagination.Page<ResourceTemplate> listTemplates(String cursor);

    Closeable subscribe(String uri, Consumer<ResourceUpdate> listener);

    default boolean supportsSubscribe() {
        return false;
    }
}
