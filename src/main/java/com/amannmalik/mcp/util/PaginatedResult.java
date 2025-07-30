package com.amannmalik.mcp.util;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

/**
 * Common pagination information for results.
 */
public record PaginatedResult(String nextCursor, JsonObject _meta) {
    public PaginatedResult {
        MetaValidator.requireValid(_meta);
    }
}
