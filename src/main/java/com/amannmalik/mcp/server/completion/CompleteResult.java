package com.amannmalik.mcp.server.completion;

import java.util.List;

/** Response to a completion request. */
public record CompleteResult(Completion completion) {
    public CompleteResult {
        if (completion == null) throw new IllegalArgumentException("completion required");
    }

    /** Completion result payload. */
    public record Completion(List<String> values, Integer total, Boolean hasMore) {
        public Completion {
            values = values == null ? List.of() : List.copyOf(values);
        }

        @Override
        public List<String> values() {
            return List.copyOf(values);
        }
    }
}
