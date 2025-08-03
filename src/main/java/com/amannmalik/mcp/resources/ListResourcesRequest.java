package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListResourcesRequest(String cursor, JsonObject _meta) {
    public ListResourcesRequest {
        MetaValidator.requireValid(_meta);
    }
}
