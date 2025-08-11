package com.amannmalik.mcp.api;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public record CallToolRequest(String name,
                              JsonObject arguments,
                              JsonObject _meta) {

    public CallToolRequest {
        if (name == null) throw new IllegalArgumentException("name required");
        name = ValidationUtil.requireClean(name);
        arguments = arguments == null ? JsonValue.EMPTY_JSON_OBJECT : arguments;
        ValidationUtil.requireMeta(_meta);
    }

}
