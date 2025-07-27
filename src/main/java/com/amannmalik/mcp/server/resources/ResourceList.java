package com.amannmalik.mcp.server.resources;

import java.util.List;

public record ResourceList(List<Resource> resources, String nextCursor) {
    public ResourceList {
        resources = resources == null ? List.of() : List.copyOf(resources);
    }
}
