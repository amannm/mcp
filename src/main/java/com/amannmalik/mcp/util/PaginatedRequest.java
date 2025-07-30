package com.amannmalik.mcp.util;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

/**
 * Common pagination parameters for requests.
 */
public record PaginatedRequest(String cursor, JsonObject _meta) {
    public PaginatedRequest {
        MetaValidator.requireValid(_meta);
    }
}
