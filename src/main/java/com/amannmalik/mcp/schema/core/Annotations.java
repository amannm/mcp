package com.amannmalik.mcp.schema.core;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Optional hints about how data should be used or displayed. */
public record Annotations(
        Optional<List<Role>> audience,
        Optional<Instant> lastModified,
        Optional<Double> priority) {
    public Annotations {
        audience = audience.map(list -> List.copyOf(list.stream().filter(r -> r != null).toList()));
        priority.ifPresent(p -> {
            if (p < 0 || p > 1) throw new IllegalArgumentException("priority out of range");
        });
    }
}
