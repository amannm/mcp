package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

/// - [Tools](specification/2025-06-18/server/tools.mdx)
/// - [MCP tools specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:38-56)
public non-sealed interface ToolProvider extends ExecutingProvider<Tool, ToolResult> {
    ToolResult call(String name, JsonObject arguments);

    @Override
    default ToolResult execute(String name, JsonObject args) {
        return call(name, args);
    }
}
