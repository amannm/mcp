package com.amannmalik.mcp.prompts;

import java.util.Map;


public interface PromptProvider {
    PromptPage list(String cursor);

    PromptInstance get(String name, Map<String, String> arguments);
}
