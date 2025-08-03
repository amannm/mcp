package com.amannmalik.mcp.util;

import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

public record PaginatedResult(String nextCursor, JsonObject _meta) {
    public PaginatedResult {
        MetaValidator.requireValid(_meta);
    }
}
