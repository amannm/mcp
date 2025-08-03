package com.amannmalik.mcp.server.completion;

import com.amannmalik.mcp.config.McpConfiguration;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.server.roots.validation.InputSanitizer;
import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record CompleteResult(Completion completion, JsonObject _meta) {
    public static final int MAX_VALUES =
            McpConfiguration.current().performance().pagination().maxCompletionValues();

    public CompleteResult {
        if (completion == null) throw new IllegalArgumentException("completion required");
        MetaValidator.requireValid(_meta);
    }

    public record Completion(List<String> values, Integer total, Boolean hasMore) {
        public Completion(List<String> values, Integer total, Boolean hasMore) {
            List<String> copy = Immutable.list(values).stream()
                    .map(InputSanitizer::requireClean)
                    .toList();
            this.values = copy;
            this.total = total;
            this.hasMore = hasMore;
            if (this.values.size() > MAX_VALUES) {
                throw new IllegalArgumentException("values must not exceed " + MAX_VALUES + " items");
            }
            if (this.total != null) {
                if (this.total < 0) throw new IllegalArgumentException("total must be non-negative");
                if (this.total < this.values.size()) {
                    throw new IllegalArgumentException("total must be >= values length");
                }
            }
        }
    }
}
