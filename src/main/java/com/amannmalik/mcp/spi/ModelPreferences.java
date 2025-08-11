package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;

import java.util.List;

public record ModelPreferences(
        List<ModelHint> hints,
        Double costPriority,
        Double speedPriority,
        Double intelligencePriority
) {
    public ModelPreferences {
        hints = hints == null || hints.isEmpty() ? List.of() : List.copyOf(hints);
        if (costPriority != null) {
            costPriority = ValidationUtil.requireFraction(costPriority, "costPriority");
        }
        if (speedPriority != null) {
            speedPriority = ValidationUtil.requireFraction(speedPriority, "speedPriority");
        }
        if (intelligencePriority != null) {
            intelligencePriority = ValidationUtil.requireFraction(intelligencePriority, "intelligencePriority");
        }
    }

}
