package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;

import java.util.List;

public record Completion(List<String> values, Integer total, Boolean hasMore) {
    public Completion(List<String> values, Integer total, Boolean hasMore) {
        this.values = Immutable.list(values).stream()
                .map(ValidationUtil::requireClean)
                .toList();
        this.total = total == null ? null : ValidationUtil.requireNonNegative(total, "total");
        this.hasMore = hasMore;
        if (this.values.size() > CompleteResult.MAX_VALUES) {
            throw new IllegalArgumentException("values must not exceed " + CompleteResult.MAX_VALUES + " items");
        }
        if (this.total != null && this.total < this.values.size()) {
            throw new IllegalArgumentException("total must be >= values length");
        }
    }
}
