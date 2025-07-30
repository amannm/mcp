package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public record CallToolRequest(String name, JsonObject arguments) {
    public CallToolRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = InputSanitizer.requireClean(name);
        arguments = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
    }
}
