package com.amannmalik.mcp.spi;

import jakarta.json.JsonObject;

import java.util.Optional;

/// - [Tools](specification/2025-06-18/server/tools.mdx)
/// - [MCP tools specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:38-56)
public non-sealed interface ToolProvider extends Provider<Tool> {
    ToolResult call(String name, JsonObject arguments);

    default Optional<Tool> find(String name) {
        if (name == null) throw new IllegalArgumentException("name required");
        String cursor = null;
        do {
            Pagination.Page<Tool> page = list(cursor);
            for (Tool t : page.items()) {
                if (t.name().equals(name)) return Optional.of(t);
            }
            Cursor next = page.nextCursor();
            cursor = next instanceof Cursor.Token t ? t.value() : null;
        } while (cursor != null);
        return Optional.empty();
    }
}
