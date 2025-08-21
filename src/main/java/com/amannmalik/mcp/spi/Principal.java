package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
import java.util.Set;

public record Principal(String id, Set<String> scopes) {
    public Principal {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        scopes = Immutable.set(scopes);
    }
}
