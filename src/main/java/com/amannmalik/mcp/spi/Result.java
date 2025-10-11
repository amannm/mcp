package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.ReadResourceResult;
import jakarta.json.JsonObject;

public sealed interface Result permits PaginatedResult,
        CompleteResult,
        ElicitResult,
        ListRootsResult,
        ToolResult,
        CreateMessageResponse,
        ReadResourceResult {
    JsonObject _meta();
}

