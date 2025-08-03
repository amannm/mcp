package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.server.roots.validation.InputSanitizer;
import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public record CallToolRequest(String name,
                              JsonObject arguments,
                              JsonObject _meta) {
    public CallToolRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = InputSanitizer.requireClean(name);
        arguments = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
        MetaValidator.requireValid(_meta);
    }
}
