package com.amannmalik.mcp.server.tools;

import jakarta.json.JsonObject;

public interface ToolProvider {
    ToolPage list(String cursor);

    CallToolResult call(String name, JsonObject arguments);

    default ToolListSubscription subscribeList(ToolListListener listener) {
        return () -> {
        };
    }

    /**
     * Whether {@link #subscribeList(ToolListListener)} delivers notifications.
     */
    default boolean supportsListChanged() {
        return false;
    }
}
