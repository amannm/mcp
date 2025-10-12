package com.amannmalik.mcp.spi;

import java.io.Closeable;
import java.net.URI;
import java.util.Optional;
import java.util.function.Consumer;

/// - [Resources](specification/2025-06-18/server/resources.mdx)
/// - [Conformance Suite](src/test/resources/com/amannmalik/mcp/mcp.feature)
public non-sealed interface ResourceProvider extends NamedProvider<Resource> {
    ResourceBlock read(URI uri);

    Optional<Resource> get(URI uri);

    Pagination.Page<ResourceTemplate> listTemplates(Cursor cursor);

    Closeable subscribe(URI uri, Consumer<ResourceUpdate> listener);

    boolean supportsSubscribe();
}
