package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.server.roots.validation.InputSanitizer;
import com.amannmalik.mcp.server.roots.validation.UriValidator;

public record ResourceUpdate(String uri, String title) {
    public ResourceUpdate {
        uri = UriValidator.requireAbsolute(uri);
        title = InputSanitizer.cleanNullable(title);
    }
}
