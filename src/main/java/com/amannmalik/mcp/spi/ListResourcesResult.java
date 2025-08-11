package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

import java.util.List;

public record ListResourcesResult(List<Resource> resources,
                                  String nextCursor,
                                  JsonObject _meta)
        implements PaginatedResult<Resource> {
    public ListResourcesResult {
        resources = PaginatedResult.items(resources);
        _meta = PaginatedResult.meta(_meta);
    }

    @Override
    public List<Resource> items() {
        return resources;
    }
}
