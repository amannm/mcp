package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.UriValidator;

public record ResourceUpdatedNotification(String uri, String title) {
    public ResourceUpdatedNotification {
        uri = UriValidator.requireAbsolute(uri);
        title = InputSanitizer.cleanNullable(title);
    }
}
