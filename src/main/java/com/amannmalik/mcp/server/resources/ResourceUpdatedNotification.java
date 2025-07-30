package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

public record ResourceUpdatedNotification(String uri) {
    public ResourceUpdatedNotification {
        uri = UriValidator.requireAbsolute(uri);
    }
}
