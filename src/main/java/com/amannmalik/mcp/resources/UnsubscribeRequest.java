package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import com.amannmalik.mcp.server.roots.validation.UriValidator;
import jakarta.json.JsonObject;

public record UnsubscribeRequest(String uri, JsonObject _meta) {
    public UnsubscribeRequest {
        uri = UriValidator.requireAbsolute(uri);
        MetaValidator.requireValid(_meta);
    }
}
