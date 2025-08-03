package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.server.roots.validation.InputSanitizer;
import com.amannmalik.mcp.server.roots.validation.UriValidator;

public record ResourceUpdatedNotification(String uri, String title) {
    public ResourceUpdatedNotification {
        uri = UriValidator.requireAbsolute(uri);
        title = InputSanitizer.cleanNullable(title);
    }
}
