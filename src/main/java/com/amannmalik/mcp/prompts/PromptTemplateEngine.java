package com.amannmalik.mcp.prompts;

import java.util.Map;

public final class PromptTemplateEngine {
    private final PromptProvider provider;

    public PromptTemplateEngine(PromptProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider required");
        this.provider = provider;
    }

    public PromptInstance get(String name, Map<String, String> arguments) {
        if (name == null || arguments == null) {
            throw new IllegalArgumentException("name and arguments required");
        }
        return provider.get(name, arguments);
    }
}
