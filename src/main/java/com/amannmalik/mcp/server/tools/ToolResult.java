package com.amannmalik.mcp.server.tools;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

/** Result of a tool invocation. */
public record ToolResult(JsonArray content, JsonObject structuredContent, boolean isError) {
    public ToolResult {
        content = content == null ? JsonValue.EMPTY_JSON_ARRAY : content;
    }
}
