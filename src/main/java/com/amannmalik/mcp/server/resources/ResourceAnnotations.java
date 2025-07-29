package com.amannmalik.mcp.server.resources;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

public record ResourceAnnotations(Set<Audience> audience, Double priority, Instant lastModified) {
    public ResourceAnnotations {
        audience = audience == null || audience.isEmpty() ? Set.of() : EnumSet.copyOf(audience);
        if (priority != null && (priority < 0.0 || priority > 1.0)) {
            throw new IllegalArgumentException("priority must be between 0.0 and 1.0");
        }
    }

    @Override
    public Set<Audience> audience() {
        return Set.copyOf(audience);
    }
}
