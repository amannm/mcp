package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.spi.internal.SpiPreconditions;

import java.util.Set;

public record Principal(String id, Set<String> scopes) {
    public Principal {
        SpiPreconditions.requireNonNull(id, "id is required");
        scopes = SpiPreconditions.immutableSet(scopes);
    }

    @Override
    public Set<String> scopes() {
        return SpiPreconditions.copySet(scopes);
    }
}
