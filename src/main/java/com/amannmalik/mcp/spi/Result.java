package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

public sealed interface Result permits PaginatedResult,
        CompleteResult,
        ElicitResult,
        ListRootsResult,
        ToolResult,
        CreateMessageResponse,
        com.amannmalik.mcp.resources.ReadResourceResult {
    JsonObject _meta();
}

