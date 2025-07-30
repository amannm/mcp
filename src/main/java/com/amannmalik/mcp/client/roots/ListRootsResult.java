package com.amannmalik.mcp.client.roots;

import java.util.List;

public record ListRootsResult(List<Root> roots) {
    public ListRootsResult {
        roots = roots == null || roots.isEmpty() ? List.of() : List.copyOf(roots);
    }
}
