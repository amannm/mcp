package com.amannmalik.mcp.server.tools;

import jakarta.json.JsonObject;

/** Definition of a server-exposed tool. */
public record Tool(String name,
                    String title,
                    String description,
                    JsonObject inputSchema,
                    JsonObject outputSchema) {
    public Tool {
        if (name == null || inputSchema == null) {
            throw new IllegalArgumentException("name and inputSchema are required");
        }
    }
}
