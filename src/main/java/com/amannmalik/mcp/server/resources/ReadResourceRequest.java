package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

public record ReadResourceRequest(String uri) {
    public ReadResourceRequest {
        uri = UriValidator.requireAbsolute(uri);
    }
}
