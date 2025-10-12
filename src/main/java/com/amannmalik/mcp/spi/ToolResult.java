package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.ToolResultContract;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;

public record ToolResult(JsonArray content,
                         JsonObject structuredContent,
                         Boolean isError,
                         JsonObject _meta) implements Result {
    public ToolResult {
        content = ToolResultContract.sanitizeContent(content);
        isError = ToolResultContract.normalizeErrorFlag(isError);
        ToolResultContract.requireMeta(_meta);
    }
}
