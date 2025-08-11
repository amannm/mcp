package com.amannmalik.mcp.api.model;

import java.util.Set;

public record Principal(String id, Set<String> scopes) {
    public Principal {
        if (id == null) throw new IllegalArgumentException("id is required");
        scopes = scopes == null || scopes.isEmpty() ? Set.of() : Set.copyOf(scopes);
    }
}
