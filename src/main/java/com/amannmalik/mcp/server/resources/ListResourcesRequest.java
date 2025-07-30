package com.amannmalik.mcp.server.resources;

import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListResourcesRequest(String cursor, JsonObject _meta) {
    public ListResourcesRequest {
        MetaValidator.requireValid(_meta);
    }
}
