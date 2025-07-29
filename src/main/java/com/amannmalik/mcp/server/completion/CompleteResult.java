package com.amannmalik.mcp.server.completion;

import java.util.List;

public record CompleteResult(Completion completion) {
    public CompleteResult {
        if (completion == null) throw new IllegalArgumentException("completion required");
    }

    public record Completion(List<String> values, Integer total, Boolean hasMore) {
        public Completion {
            values = values == null ? List.of() : List.copyOf(values);
            if (values.size() > 100) {
                throw new IllegalArgumentException("values must not exceed 100 items");
            }
            if (total != null) {
                if (total < 0) throw new IllegalArgumentException("total must be non-negative");
                if (total < values.size()) {
                    throw new IllegalArgumentException("total must be >= values length");
                }
            }
        }

        @Override
        public List<String> values() {
            return List.copyOf(values);
        }
    }
}
