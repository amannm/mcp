package com.amannmalik.mcp.client.sampling;

import java.util.List;


public record ModelPreferences(
        List<ModelHint> hints,
        Double costPriority,
        Double speedPriority,
        Double intelligencePriority
) {
    public ModelPreferences {
        hints = hints == null || hints.isEmpty() ? List.of() : List.copyOf(hints);
        if (costPriority != null && (costPriority < 0.0 || costPriority > 1.0)) {
            throw new IllegalArgumentException("costPriority must be between 0.0 and 1.0");
        }
        if (speedPriority != null && (speedPriority < 0.0 || speedPriority > 1.0)) {
            throw new IllegalArgumentException("speedPriority must be between 0.0 and 1.0");
        }
        if (intelligencePriority != null && (intelligencePriority < 0.0 || intelligencePriority > 1.0)) {
            throw new IllegalArgumentException("intelligencePriority must be between 0.0 and 1.0");
        }
    }
}
