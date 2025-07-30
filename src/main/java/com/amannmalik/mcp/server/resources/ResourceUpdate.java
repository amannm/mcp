package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

/**
 * Internal representation of a resource update event.
 */
public record ResourceUpdate(String uri) {
    public ResourceUpdate {
        uri = UriValidator.requireAbsolute(uri);
    }
}
