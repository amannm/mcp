package com.amannmalik.mcp.prompts;

import java.util.Map;


public interface PromptProvider {
    PromptPage list(String cursor);

    PromptInstance get(String name, Map<String, String> arguments);

    /**
     * Subscribe to prompt list changes.
     *
     * Implementations that do not support subscriptions return a no-op handle
     * that can be safely closed.
     */
    default PromptsSubscription subscribe(PromptsListener listener) {
        return () -> { };
    }
}
