package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.validation.*;
import jakarta.json.JsonObject;

public record ResourceTemplate(
        String uriTemplate,
        String name,
        String title,
        String description,
        String mimeType,
        Annotations annotations,
        JsonObject _meta
) implements DisplayNameProvider {
    public ResourceTemplate {
        uriTemplate = UriTemplateValidator.requireAbsoluteTemplate(uriTemplate);
        name = InputSanitizer.requireClean(name);
        title = InputSanitizer.cleanNullable(title);
        description = InputSanitizer.cleanNullable(description);
        mimeType = InputSanitizer.cleanNullable(mimeType);
        annotations = annotations == null ? Annotations.EMPTY : annotations;
        MetaValidator.requireValid(_meta);
    }

}
