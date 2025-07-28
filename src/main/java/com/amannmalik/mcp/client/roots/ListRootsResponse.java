package com.amannmalik.mcp.client.roots;

import java.util.List;

/** Response to a ListRootsRequest. */
public record ListRootsResponse(List<Root> roots) {
    public ListRootsResponse {
        roots = roots == null || roots.isEmpty() ? List.of() : List.copyOf(roots);
    }

    @Override
    public List<Root> roots() {
        return List.copyOf(roots);
    }
}
