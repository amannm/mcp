package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.spi.internal.SpiPreconditions;
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
        uri = SpiPreconditions.requireAbsoluteUri(uri);
        name = SpiPreconditions.requireClean(name);
        title = SpiPreconditions.cleanNullable(title);
        description = SpiPreconditions.cleanNullable(description);
        mimeType = SpiPreconditions.cleanNullable(mimeType);
        size = SpiPreconditions.nonNegativeOrNull(size, "size");
        annotations = SpiPreconditions.annotationsOrEmpty(annotations);
        SpiPreconditions.requireMeta(_meta);
    }

}
