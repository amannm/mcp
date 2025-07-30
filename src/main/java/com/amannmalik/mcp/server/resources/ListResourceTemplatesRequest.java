package com.amannmalik.mcp.server.resources;

import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListResourceTemplatesRequest(String cursor, JsonObject _meta) {
    public ListResourceTemplatesRequest {
        MetaValidator.requireValid(_meta);
    }
}
