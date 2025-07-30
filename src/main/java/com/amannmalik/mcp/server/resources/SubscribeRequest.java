package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

public record SubscribeRequest(String uri) {
    public SubscribeRequest {
        uri = UriValidator.requireAbsolute(uri);
    }
}
