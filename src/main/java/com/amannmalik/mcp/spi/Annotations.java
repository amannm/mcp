package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;

import java.time.Instant;
import java.util.Set;

public record Annotations(Set<Role> audience, Double priority, Instant lastModified) {
    public Annotations {
        audience = Immutable.enumSet(audience);
        if (priority != null) {
            priority = ValidationUtil.requireFraction(priority, "priority");
        }
    }
}
