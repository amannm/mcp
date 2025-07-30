package com.amannmalik.mcp.client.roots;

import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListRootsRequest(JsonObject _meta) {
    public ListRootsRequest {
        MetaValidator.requireValid(_meta);
    }
}
