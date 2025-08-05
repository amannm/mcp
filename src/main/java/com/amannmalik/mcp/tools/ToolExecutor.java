package com.amannmalik.mcp.tools;

import jakarta.json.JsonObject;

public final class ToolExecutor {
    private final ToolProvider provider;

    public ToolExecutor(ToolProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider required");
        this.provider = provider;
    }

    public ToolResult execute(String name, JsonObject arguments) {
        if (name == null || arguments == null) {
            throw new IllegalArgumentException("name and arguments required");
        }
        return provider.call(name, arguments);
    }
}
