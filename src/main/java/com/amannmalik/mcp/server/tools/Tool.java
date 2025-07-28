package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.validation.InputSanitizer;

import jakarta.json.JsonObject;

/** Definition of a server-exposed tool. */
public record Tool(String name,
                    String title,
                    String description,
                    JsonObject inputSchema,
                    JsonObject outputSchema) {
    public Tool {
        name = InputSanitizer.requireClean(name);
        if (inputSchema == null) {
            throw new IllegalArgumentException("inputSchema is required");
        }
        title = title == null ? null : InputSanitizer.requireClean(title);
        description = description == null ? null : InputSanitizer.requireClean(description);
    }
}
