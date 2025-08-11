package com.amannmalik.mcp.spi;

/// - [Pagination](specification/2025-06-18/server/utilities/pagination.mdx)
public sealed interface PaginatedResult extends Result permits ListResourceTemplatesResult, ListResourcesResult, ListToolsResult {
    String nextCursor();
}

