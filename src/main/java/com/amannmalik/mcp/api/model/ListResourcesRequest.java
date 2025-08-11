package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListResourcesRequest(String cursor, JsonObject _meta) {
    public ListResourcesRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
