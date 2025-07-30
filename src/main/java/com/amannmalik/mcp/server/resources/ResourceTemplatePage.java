package com.amannmalik.mcp.server.resources;

import java.util.List;

public record ResourceTemplatePage(List<ResourceTemplate> resourceTemplates, String nextCursor) {
    public ResourceTemplatePage {
        resourceTemplates = resourceTemplates == null ? List.of() : List.copyOf(resourceTemplates);
    }
}
