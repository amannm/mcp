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
    }
}
