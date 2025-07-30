package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.UriValidator;

/** Request for a {@code resources/read} call. */
public record ReadResourceRequest(String uri) {
    public ReadResourceRequest {
        uri = UriValidator.requireAbsolute(uri);
    }
}
