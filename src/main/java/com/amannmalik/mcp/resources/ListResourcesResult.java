package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourcesResult(List<Resource> resources,
                                  String nextCursor,
                                  JsonObject _meta) {
    public ListResourcesResult {
        resources = Immutable.list(resources);
        MetaValidator.requireValid(_meta);
    }
}
