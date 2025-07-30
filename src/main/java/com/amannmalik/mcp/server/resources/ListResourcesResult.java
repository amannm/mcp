package com.amannmalik.mcp.server.resources;

import java.util.List;


public record ListResourcesResult(List<Resource> resources, String nextCursor) {
    public ListResourcesResult {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
