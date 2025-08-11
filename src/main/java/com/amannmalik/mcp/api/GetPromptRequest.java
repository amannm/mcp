package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.Map;

public record GetPromptRequest(String name,
                               Map<String, String> arguments,
                               JsonObject _meta) {
    public GetPromptRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = ValidationUtil.requireClean(name);
        arguments = ValidationUtil.requireCleanMap(arguments);
        ValidationUtil.requireMeta(_meta);
    }
}
