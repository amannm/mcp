package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

import java.util.List;

public record ListToolsResult(List<Tool> tools,
                              String nextCursor,
                              JsonObject _meta)
        implements PaginatedResult<Tool> {
    public ListToolsResult {
        tools = PaginatedResult.items(tools);
        _meta = PaginatedResult.meta(_meta);
    }

    @Override
    public List<Tool> items() {
        return tools;
    }
}
