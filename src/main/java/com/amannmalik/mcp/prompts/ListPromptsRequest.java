package com.amannmalik.mcp.prompts;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record ListPromptsRequest(String cursor, JsonObject _meta) {
    public ListPromptsRequest {
        MetaValidator.requireValid(_meta);
    }
}
