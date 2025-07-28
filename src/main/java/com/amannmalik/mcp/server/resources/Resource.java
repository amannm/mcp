package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.UriValidator;

public record Resource(
        String uri,
        String name,
        String title,
        String description,
        String mimeType,
        Long size,
        ResourceAnnotations annotations
) {
    public Resource {
        uri = UriValidator.requireAbsolute(uri);
        name = InputSanitizer.requireClean(name);
        title = title == null ? null : InputSanitizer.requireClean(title);
        description = description == null ? null : InputSanitizer.requireClean(description);
        mimeType = mimeType == null ? null : InputSanitizer.requireClean(mimeType);
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
    }
}
