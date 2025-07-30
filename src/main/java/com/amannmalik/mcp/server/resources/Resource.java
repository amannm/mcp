package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.MetaValidator;
import com.amannmalik.mcp.validation.UriValidator;
import jakarta.json.JsonObject;

public record Resource(
        String uri,
        String name,
        String title,
        String description,
        String mimeType,
        Long size,
        Annotations annotations,
        JsonObject _meta
) {
    public Resource {
        uri = UriValidator.requireAbsolute(uri);
        name = InputSanitizer.requireClean(name);
        title = InputSanitizer.cleanNullable(title);
        description = InputSanitizer.cleanNullable(description);
        mimeType = InputSanitizer.cleanNullable(mimeType);
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        MetaValidator.requireValid(_meta);
    }
}
