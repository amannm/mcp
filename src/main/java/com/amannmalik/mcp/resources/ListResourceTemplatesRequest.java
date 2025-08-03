package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListResourceTemplatesRequest(String cursor, JsonObject _meta) {
    public ListResourceTemplatesRequest {
        MetaValidator.requireValid(_meta);
    }
}
