package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

public record UnsubscribeRequest(String uri) {
    public UnsubscribeRequest {
        uri = UriValidator.requireAbsolute(uri);
    }
}
