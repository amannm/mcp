package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.InputSanitizer;
import com.amannmalik.mcp.validation.UriTemplateValidator;

public record ResourceTemplate(
        String uriTemplate,
        String name,
        String title,
        String description,
        String mimeType,
        ResourceAnnotations annotations
) {
    public ResourceTemplate {
        uriTemplate = UriTemplateValidator.requireAbsoluteTemplate(uriTemplate);
        name = InputSanitizer.requireClean(name);
        title = title == null ? null : InputSanitizer.requireClean(title);
        description = description == null ? null : InputSanitizer.requireClean(description);
        mimeType = mimeType == null ? null : InputSanitizer.requireClean(mimeType);
    }
}
