package com.amannmalik.mcp.server.tools;

import jakarta.json.JsonObject;


public interface ToolProvider {
    ToolPage list(String cursor);

    ToolResult call(String name, JsonObject arguments);
}
