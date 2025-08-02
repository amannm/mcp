package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.UriValidator;

public record ResourceUpdate(String uri, String title) {
    public ResourceUpdate {
        uri = UriValidator.requireAbsolute(uri);
        title = InputSanitizer.cleanNullable(title);
    }
}
