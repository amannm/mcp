package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListRootsRequest(JsonObject _meta) {
    public ListRootsRequest {
        MetaValidator.requireValid(_meta);
    }
}
