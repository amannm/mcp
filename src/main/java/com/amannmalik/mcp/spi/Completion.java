package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.spi.internal.CompletionContract;
import com.amannmalik.mcp.spi.internal.SpiPreconditions;

import java.util.List;

public record Completion(List<String> values, Integer total, Boolean hasMore) {
    public Completion {
        values = CompletionContract.sanitizeValues(values);
        total = CompletionContract.normalizeTotal(total, values.size());
    }

    @Override
    public List<String> values() {
        return SpiPreconditions.copyList(values);
    }
}
