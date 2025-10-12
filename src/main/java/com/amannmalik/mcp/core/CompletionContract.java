package com.amannmalik.mcp.core;

import com.amannmalik.mcp.util.ValidationUtil;

import java.util.List;

public final class CompletionContract {
    private CompletionContract() {
    }

    public static List<String> sanitizeValues(List<String> values) {
        return ValidationUtil.immutableList(values)
                .stream()
                .map(ValidationUtil::requireClean)
                .toList();
    }

    public static Integer normalizeTotal(Integer total, int valuesSize) {
        if (total == null) {
            return null;
        }
        var sanitized = ValidationUtil.requireNonNegative(total, "total");
        if (sanitized < valuesSize) {
            throw new IllegalArgumentException("total must be >= values length");
        }
        return sanitized;
    }
}
