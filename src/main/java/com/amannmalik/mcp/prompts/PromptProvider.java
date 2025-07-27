package com.amannmalik.mcp.prompts;

import java.util.List;
import java.util.Map;

/** Mechanism for a server to expose prompts to clients. */
public interface PromptProvider {
    List<Prompt> listPrompts();

    PromptInstance getPrompt(String name, Map<String, String> arguments);
}
