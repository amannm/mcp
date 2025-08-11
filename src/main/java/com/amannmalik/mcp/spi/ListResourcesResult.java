package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourcesResult(List<Resource> resources,
                                  String nextCursor,
                                  JsonObject _meta) implements PaginatedResult {
    public ListResourcesResult {
        resources = Immutable.list(resources);
        ValidationUtil.requireMeta(_meta);
    }
}
