package com.amannmalik.mcp.ping;

import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record PingRequest(JsonObject _meta) {
    public PingRequest {
        MetaValidator.requireValid(_meta);
    }
}
