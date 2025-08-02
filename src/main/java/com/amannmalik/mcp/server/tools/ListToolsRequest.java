package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListToolsRequest(String cursor, JsonObject _meta) {
    public ListToolsRequest {
        MetaValidator.requireValid(_meta);
    }
}
