package com.amannmalik.mcp.server.resources;

import java.util.List;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates, String nextCursor) {
    public ListResourceTemplatesResult {
        resourceTemplates = resourceTemplates == null ? List.of() : List.copyOf(resourceTemplates);
    }
}
