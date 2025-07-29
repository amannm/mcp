package com.amannmalik.mcp.prompts;

import java.util.Map;

public interface PromptProvider {
    PromptPage list(String cursor);

    PromptInstance get(String name, Map<String, String> arguments);

    default PromptsSubscription subscribe(PromptsListener listener) {
        return () -> {
        };
    }

    /**
     * Whether {@link #subscribe(PromptsListener)} delivers notifications.
     */
    default boolean supportsListChanged() {
        return false;
    }
}
