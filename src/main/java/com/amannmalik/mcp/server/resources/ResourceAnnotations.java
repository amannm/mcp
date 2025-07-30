package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.prompts.Role;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

public record ResourceAnnotations(Set<Role> audience, Double priority, Instant lastModified) {
    public ResourceAnnotations {
        audience = audience == null || audience.isEmpty() ? Set.of() : EnumSet.copyOf(audience);
        if (priority != null && (priority < 0.0 || priority > 1.0)) {
            throw new IllegalArgumentException("priority must be between 0.0 and 1.0");
        }
    }

    @Override
    public Set<Role> audience() {
        return Set.copyOf(audience);
    }
}
