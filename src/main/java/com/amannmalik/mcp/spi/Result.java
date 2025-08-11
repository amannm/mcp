package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

/// - [Specification](specification/2025-06-18/index.mdx)
public sealed interface Result permits CompleteResult, ElicitResult, ListRootsResult, PaginatedResult, ToolResult {
    JsonObject _meta();
}

