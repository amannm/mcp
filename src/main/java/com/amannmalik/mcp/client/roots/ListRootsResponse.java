package com.amannmalik.mcp.client.roots;

import java.util.List;

public record ListRootsResponse(List<Root> roots) {
    public ListRootsResponse {
        roots = roots == null || roots.isEmpty() ? List.of() : List.copyOf(roots);
    }
}
