package com.amannmalik.mcp.ping;

import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

public record PingRequest(JsonObject _meta) {
    public PingRequest {
        MetaValidator.requireValid(_meta);
    }
}
