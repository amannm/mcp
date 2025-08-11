package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListResourceTemplatesRequest(String cursor, JsonObject _meta) {
    public ListResourceTemplatesRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
