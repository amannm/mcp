package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

public record SubscribeRequest(String uri, JsonObject _meta) {
    public SubscribeRequest {
        uri = UriValidator.requireAbsolute(uri);
        MetaValidator.requireValid(_meta);
    }
}
