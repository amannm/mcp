package com.amannmalik.mcp.prompts;

import java.util.Map;

/** Mechanism for a server to expose prompts to clients. */
public interface PromptProvider {
    PromptPage list(String cursor);

    PromptInstance get(String name, Map<String, String> arguments);
}
