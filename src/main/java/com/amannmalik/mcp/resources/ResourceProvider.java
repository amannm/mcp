package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.Provider;
import com.amannmalik.mcp.util.*;

import java.util.Optional;

/// - [Resources](specification/2025-06-18/server/resources.mdx)
/// - [MCP Conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:83-95)
public interface ResourceProvider extends Provider<Resource> {
    ResourceBlock read(String uri);

    default Optional<Resource> get(String uri) {
        return Optional.empty();
    }

    Pagination.Page<ResourceTemplate> listTemplates(String cursor);

    default ChangeSubscription subscribe(String uri, ChangeListener<ResourceUpdate> listener) {
        return () -> {
        };
    }

    default boolean supportsSubscribe() {
        return false;
    }
}
