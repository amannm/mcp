package com.amannmalik.mcp.server.resources;

import java.util.List;

/** Result of a resource template listing request. */
public record ResourceTemplatePage(List<ResourceTemplate> resourceTemplates, String nextCursor) {
    public ResourceTemplatePage {
        resourceTemplates = resourceTemplates == null ? List.of() : List.copyOf(resourceTemplates);
    }

    @Override
    public List<ResourceTemplate> resourceTemplates() {
        return List.copyOf(resourceTemplates);
    }
}
