package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.SpiPreconditions;

import java.util.List;

public record ModelPreferences(
        List<ModelHint> hints,
        Double costPriority,
        Double speedPriority,
        Double intelligencePriority
) {
    public ModelPreferences {
        hints = SpiPreconditions.immutableList(hints);
        costPriority = SpiPreconditions.fractionOrNull(costPriority, "costPriority");
        speedPriority = SpiPreconditions.fractionOrNull(speedPriority, "speedPriority");
        intelligencePriority = SpiPreconditions.fractionOrNull(intelligencePriority, "intelligencePriority");
    }

    @Override
    public List<ModelHint> hints() {
        return SpiPreconditions.copyList(hints);
    }
}
