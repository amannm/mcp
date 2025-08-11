package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

import java.util.List;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          String nextCursor,
                                          JsonObject _meta)
        implements PaginatedResult<ResourceTemplate> {
    public ListResourceTemplatesResult {
        resourceTemplates = PaginatedResult.items(resourceTemplates);
        _meta = PaginatedResult.meta(_meta);
    }

    @Override
    public List<ResourceTemplate> items() {
        return resourceTemplates;
    }
}
