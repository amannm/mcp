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
        hints = ValidationUtil.immutableList(hints);
        costPriority = ValidationUtil.fractionOrNull(costPriority, "costPriority");
        speedPriority = ValidationUtil.fractionOrNull(speedPriority, "speedPriority");
        intelligencePriority = ValidationUtil.fractionOrNull(intelligencePriority, "intelligencePriority");
    }

    @Override
    public List<ModelHint> hints() {
        return ValidationUtil.copyList(hints);
    }
}
