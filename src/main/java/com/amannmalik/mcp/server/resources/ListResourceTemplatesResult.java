package com.amannmalik.mcp.server.resources;

import java.util.List;

/** Result for a {@code resources/templates/list} request. */
public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates, String nextCursor) {
    public ListResourceTemplatesResult {
        resourceTemplates = resourceTemplates == null ? List.of() : List.copyOf(resourceTemplates);
    }

    public List<ResourceTemplate> resourceTemplates() {
        return List.copyOf(resourceTemplates);
    }
}
