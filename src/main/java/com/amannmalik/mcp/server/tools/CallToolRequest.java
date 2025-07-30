package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.Json;
import jakarta.json.JsonObject;

/** Request for a {@code tools/call} call. */
public record CallToolRequest(String name, JsonObject arguments) {
    public CallToolRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = InputSanitizer.requireClean(name);
        arguments = arguments == null ? Json.createObjectBuilder().build() : arguments;
    }
}
