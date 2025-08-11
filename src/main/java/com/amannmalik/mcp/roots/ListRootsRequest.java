package com.amannmalik.mcp.roots;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ListRootsRequest(JsonObject _meta) {

    public ListRootsRequest {
        ValidationUtil.requireMeta(_meta);
    }
}
