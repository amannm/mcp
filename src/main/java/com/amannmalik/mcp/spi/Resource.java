package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.net.URI;

public record Resource(
        URI uri,
        String name,
        String title,
        String description,
        String mimeType,
        Long size,
        Annotations annotations,
        JsonObject _meta
) implements DisplayNameProvider {
    public Resource {
        uri = ValidationUtil.requireAbsoluteUri(uri);
        name = ValidationUtil.requireClean(name);
        title = ValidationUtil.cleanNullable(title);
        description = ValidationUtil.cleanNullable(description);
        mimeType = ValidationUtil.cleanNullable(mimeType);
        size = ValidationUtil.nonNegativeOrNull(size, "size");
        annotations = ValidationUtil.annotationsOrEmpty(annotations);
        ValidationUtil.requireMeta(_meta);
    }
}
