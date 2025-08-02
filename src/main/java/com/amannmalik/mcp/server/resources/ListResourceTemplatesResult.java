package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          String nextCursor,
                                          JsonObject _meta) {
    public ListResourceTemplatesResult {
        resourceTemplates = Immutable.list(resourceTemplates);
        MetaValidator.requireValid(_meta);
    }
}
