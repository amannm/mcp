package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.util.Pagination;

import java.util.Map;

public interface PromptProvider {
    Pagination.Page<Prompt> list(String cursor);

    PromptInstance get(String name, Map<String, String> arguments);

    default PromptsSubscription subscribe(PromptsListener listener) {
        return () -> {
        };
    }

    default boolean supportsListChanged() {
        return false;
    }
}
