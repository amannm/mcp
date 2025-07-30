package com.amannmalik.mcp.server.resources;

import java.util.List;
import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;


public record ListResourcesResult(List<Resource> resources,
                                  String nextCursor,
                                  JsonObject _meta) {
    public ListResourcesResult {
        resources = resources == null ? List.of() : List.copyOf(resources);
        MetaValidator.requireValid(_meta);
    }
}
