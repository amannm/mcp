package com.amannmalik.mcp.tools;

import com.amannmalik.mcp.util.ListChangeSubscription;
import com.amannmalik.mcp.util.Pagination;
import jakarta.json.JsonObject;

import java.util.Optional;

/// - [Tools](specification/2025-06-18/server/tools.mdx)
/// - [MCP tools specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:38-56)
public interface ToolProvider {
    Pagination.Page<Tool> list(String cursor);

    ToolResult call(String name, JsonObject arguments);

    default Optional<Tool> find(String name) {
        if (name == null) throw new IllegalArgumentException("name required");
        String cursor = null;
        do {
            Pagination.Page<Tool> page = list(cursor);
            for (Tool t : page.items()) {
                if (t.name().equals(name)) {
                    return Optional.of(t);
                }
            }
            cursor = page.nextCursor();
        } while (cursor != null);
        return Optional.empty();
    }

    default ListChangeSubscription subscribeList(ToolListListener listener) {
        return () -> {
        };
    }

    default boolean supportsListChanged() {
        return false;
    }
}
