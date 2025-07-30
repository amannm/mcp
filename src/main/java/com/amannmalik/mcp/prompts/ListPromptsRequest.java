package com.amannmalik.mcp.prompts;

import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListPromptsRequest(String cursor, JsonObject _meta) {
    public ListPromptsRequest {
        MetaValidator.requireValid(_meta);
    }
}
