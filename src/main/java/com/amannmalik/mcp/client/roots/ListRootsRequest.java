package com.amannmalik.mcp.client.roots;

import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListRootsRequest(JsonObject _meta) {
    public ListRootsRequest {
        MetaValidator.requireValid(_meta);
    }
}
