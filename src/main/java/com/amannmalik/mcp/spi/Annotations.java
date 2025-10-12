package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.SpiPreconditions;

import java.time.Instant;
import java.util.Set;

public record Annotations(Set<Role> audience, Double priority, Instant lastModified) {
    public Annotations {
        audience = SpiPreconditions.immutableEnumSet(audience);
        priority = SpiPreconditions.fractionOrNull(priority, "priority");
    }

    @Override
    public Set<Role> audience() {
        return SpiPreconditions.copySet(audience);
    }
}
