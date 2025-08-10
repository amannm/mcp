package com.amannmalik.mcp.api.prompt;

import com.amannmalik.mcp.core.Provider;
import com.amannmalik.mcp.prompts.Prompt;

import java.util.Map;

/// - [Prompts](specification/2025-06-18/server/prompts.mdx)
/// - [MCP prompts specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:60-79)
public interface PromptProvider extends Provider<Prompt> {
    PromptInstance get(String name, Map<String, String> arguments);
}
