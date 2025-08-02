package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.annotations.Annotations;
import com.amannmalik.mcp.util.DisplayNameProvider;
import com.amannmalik.mcp.validation.*;
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
) implements DisplayNameProvider, ResourceDescriptor {
    public Resource {
        uri = UriValidator.requireAbsolute(uri);
        name = InputSanitizer.requireClean(name);
        title = InputSanitizer.cleanNullable(title);
        description = InputSanitizer.cleanNullable(description);
        mimeType = InputSanitizer.cleanNullable(mimeType);
        if (size != null && size < 0) {
            throw new IllegalArgumentException("size must be >= 0");
        }
        annotations = annotations == null ? Annotations.EMPTY : annotations;
        MetaValidator.requireValid(_meta);
    }

}
