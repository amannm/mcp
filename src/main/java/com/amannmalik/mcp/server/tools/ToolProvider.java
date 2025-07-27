package com.amannmalik.mcp.server.tools;

import jakarta.json.JsonObject;

/** Service exposing tools to clients. */
public interface ToolProvider {
    ToolPage list(String cursor);

    ToolResult call(String name, JsonObject arguments);
}
