package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.SpiPreconditions;

import java.util.List;

public final class CompletionContract {
    private CompletionContract() {
    }

    public static List<String> sanitizeValues(List<String> values) {
        return SpiPreconditions.immutableList(values)
                .stream()
                .map(SpiPreconditions::requireClean)
                .toList();
    }

    public static Integer normalizeTotal(Integer total, int valuesSize) {
        if (total == null) {
            return null;
        }
        var sanitized = SpiPreconditions.requireNonNegative(total, "total");
        if (sanitized < valuesSize) {
            throw new IllegalArgumentException("total must be >= values length");
        }
        return sanitized;
    }
}
