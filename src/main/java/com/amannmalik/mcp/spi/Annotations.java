package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

import java.time.Instant;
import java.util.Set;

public record Annotations(Set<Role> audience, Double priority, Instant lastModified) {
    public Annotations {
        audience = ValidationUtil.immutableEnumSet(audience);
        priority = ValidationUtil.fractionOrNull(priority, "priority");
    }

    @Override
    public Set<Role> audience() {
        return ValidationUtil.copySet(audience);
    }
}
