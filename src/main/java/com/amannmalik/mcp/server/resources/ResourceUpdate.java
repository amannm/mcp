package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

public record ResourceUpdate(String uri) {
    public ResourceUpdate {
        uri = UriValidator.requireAbsolute(uri);
    }
}
