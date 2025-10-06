package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;

import java.util.List;

public record ModelPreferences(
        List<ModelHint> hints,
        Double costPriority,
        Double speedPriority,
        Double intelligencePriority
) {
    public ModelPreferences {
        hints = Immutable.list(hints);
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

    @Override
    public List<ModelHint> hints() {
        return List.copyOf(hints);
    }
}
