package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

public record Annotations(Set<Role> audience, Double priority, Instant lastModified) {
    public Annotations {
        audience = audience == null || audience.isEmpty() ? Set.of() : EnumSet.copyOf(audience);
        if (priority != null) priority = ValidationUtil.requireFraction(priority, "priority");
    }

    @Override
    public Set<Role> audience() {
        return Set.copyOf(audience);
    }

}
