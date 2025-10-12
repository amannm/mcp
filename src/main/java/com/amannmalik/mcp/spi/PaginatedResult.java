package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.ListPromptsResult;

import java.util.List;

public sealed interface PaginatedResult<T> extends Result permits ListResourcesResult,
        ListResourceTemplatesResult,
        ListToolsResult,
        ListPromptsResult {
    List<T> items();

    Cursor nextCursor();
}
