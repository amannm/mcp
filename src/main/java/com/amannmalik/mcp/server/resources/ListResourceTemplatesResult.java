package com.amannmalik.mcp.server.resources;

import java.util.List;
import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          String nextCursor,
                                          JsonObject _meta) {
    public ListResourceTemplatesResult {
        resourceTemplates = resourceTemplates == null ? List.of() : List.copyOf(resourceTemplates);
        MetaValidator.requireValid(_meta);
    }
}
