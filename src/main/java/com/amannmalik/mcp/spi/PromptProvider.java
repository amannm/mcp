package com.amannmalik.mcp.spi;

import java.util.Map;

/// - [Prompts](specification/2025-06-18/server/prompts.mdx)
/// - [MCP prompts specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:60-79)
public non-sealed interface PromptProvider extends NamedProvider<Prompt> {
    PromptInstance get(String name, Map<String, String> arguments);
}
