package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;


public record ListResourcesResult(List<Resource> resources,
                                  String nextCursor,
                                  JsonObject _meta) {
    public ListResourcesResult {
        resources = resources == null ? List.of() : List.copyOf(resources);
        MetaValidator.requireValid(_meta);
    }
}
