package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.server.resources.Audience;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/** Metadata about tool behavior. */
public record ToolAnnotations(Set<Audience> audience, Double priority, Instant lastModified) {
    public ToolAnnotations {
        audience = audience == null || audience.isEmpty() ? Set.of() : EnumSet.copyOf(audience);
    }

    @Override
    public Set<Audience> audience() {
        return Set.copyOf(audience);
    }
}
