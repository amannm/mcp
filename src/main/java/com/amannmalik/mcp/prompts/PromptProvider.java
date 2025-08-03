package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.util.ListChangeSubscription;
import com.amannmalik.mcp.util.Pagination;

import java.util.Map;

/// - [Prompts](specification/2025-06-18/server/prompts.mdx)
/// - [MCP prompts specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:60-79)
public interface PromptProvider {
    Pagination.Page<Prompt> list(String cursor);

    PromptInstance get(String name, Map<String, String> arguments);

    default ListChangeSubscription subscribe(PromptsListener listener) {
        return () -> {
        };
    }

    default boolean supportsListChanged() {
        return false;
    }
}
