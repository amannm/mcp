package com.amannmalik.mcp.spi;

import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

/// - [Resources](specification/2025-06-18/server/resources.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
public non-sealed interface ResourceProvider extends NamedProvider<Resource> {
    ResourceBlock read(URI uri);

    default Optional<Resource> get(URI uri) {
        return Optional.empty();
    }

    Pagination.Page<ResourceTemplate> listTemplates(Cursor cursor);

    AutoCloseable subscribe(URI uri, Consumer<ResourceUpdate> listener);

    default boolean supportsSubscribe() {
        return false;
    }
}
