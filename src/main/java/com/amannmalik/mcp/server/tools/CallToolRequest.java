package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.JsonObject;

public record CallToolRequest(String name, JsonObject arguments) {
    public CallToolRequest {
        name = InputSanitizer.requireClean(name);
    }
}
