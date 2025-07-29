package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.validation.InputSanitizer;
import java.util.List;

public record CompleteResult(Completion completion) {
    public CompleteResult {
        if (completion == null) throw new IllegalArgumentException("completion required");
    }

    public record Completion(List<String> values, Integer total, Boolean hasMore) {
        public Completion {
            if (values == null || values.isEmpty()) {
                values = List.of();
            } else {
                values = values.stream()
                        .map(InputSanitizer::requireClean)
                        .toList();
            }
        }

        @Override
        public List<String> values() {
            return List.copyOf(values);
        }
    }
}
