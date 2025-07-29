package com.amannmalik.mcp.server.tools;

import jakarta.json.JsonObject;


public interface ToolProvider {
    ToolPage list(String cursor);

    ToolResult call(String name, JsonObject arguments);

    /**
     * Subscribe to changes in the list of tools.
     * <p>
     * Implementations that do not support list change notifications may
     * return a no-op subscription.
     */
    default ToolListSubscription subscribeList(ToolListListener listener) {
        return () -> {
        };
    }
}
