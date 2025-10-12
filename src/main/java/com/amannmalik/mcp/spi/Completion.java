package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.CompletionContract;
import com.amannmalik.mcp.util.ValidationUtil;

import java.util.List;

public record Completion(List<String> values, Integer total, Boolean hasMore) {
    public Completion {
        values = CompletionContract.sanitizeValues(values);
        total = CompletionContract.normalizeTotal(total, values.size());
    }

    @Override
    public List<String> values() {
        return ValidationUtil.copyList(values);
    }
}
