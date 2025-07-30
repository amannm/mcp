package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.util.Pagination;
import jakarta.json.JsonObject;

public interface ToolProvider {
    Pagination.Page<Tool> list(String cursor);

    ToolResult call(String name, JsonObject arguments);

    default ToolListSubscription subscribeList(ToolListListener listener) {
        return () -> {
        };
    }

    default boolean supportsListChanged() {
        return false;
    }
}
