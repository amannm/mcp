package com.amannmalik.mcp.client.roots;

import java.util.List;
import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListRootsResult(List<Root> roots, JsonObject _meta) {
    public ListRootsResult {
        roots = roots == null || roots.isEmpty() ? List.of() : List.copyOf(roots);
        MetaValidator.requireValid(_meta);
    }
}
