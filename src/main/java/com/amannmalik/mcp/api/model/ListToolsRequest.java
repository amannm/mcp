package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListToolsRequest(String cursor, JsonObject _meta) {
    public ListToolsRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
