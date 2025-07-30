package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record CompleteResult(Completion completion, JsonObject _meta) {
    public CompleteResult {
        if (completion == null) throw new IllegalArgumentException("completion required");
        MetaValidator.requireValid(_meta);
    }

    public record Completion(List<String> values, Integer total, Boolean hasMore) {
        public Completion(List<String> values, Integer total, Boolean hasMore) {
            List<String> copy = values == null ? List.of() : List.copyOf(values);
            copy = copy.stream()
                    .map(InputSanitizer::requireClean)
                    .toList();
            this.values = copy;
            this.total = total;
            this.hasMore = hasMore;
            if (this.values.size() > 100) {
                throw new IllegalArgumentException("values must not exceed 100 items");
            }
            if (this.total != null) {
                if (this.total < 0) throw new IllegalArgumentException("total must be non-negative");
                if (this.total < this.values.size()) {
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
