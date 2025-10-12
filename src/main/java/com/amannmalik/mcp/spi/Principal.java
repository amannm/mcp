package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

import java.util.Set;

public record Principal(String id, Set<String> scopes) {
    public Principal {
        ValidationUtil.requireNonNull(id, "id is required");
        scopes = ValidationUtil.immutableSet(scopes);
    }

    @Override
    public Set<String> scopes() {
        return ValidationUtil.copySet(scopes);
    }
}
