package com.amannmalik.mcp.spi;

import java.util.List;

public sealed interface PaginatedResult<T> extends Result permits ListResourcesResult,
        ListResourceTemplatesResult,
        ListToolsResult,
        com.amannmalik.mcp.prompts.ListPromptsResult {
    List<T> items();

    Cursor nextCursor();
}

