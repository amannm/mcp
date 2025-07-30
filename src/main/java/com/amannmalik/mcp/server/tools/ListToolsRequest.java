package com.amannmalik.mcp.server.tools;

import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListToolsRequest(String cursor, JsonObject _meta) {
    public ListToolsRequest {
        MetaValidator.requireValid(_meta);
    }
}
