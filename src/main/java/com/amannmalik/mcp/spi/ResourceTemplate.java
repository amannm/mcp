package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.spi.internal.SpiPreconditions;
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
        uriTemplate = SpiPreconditions.requireAbsoluteTemplate(uriTemplate);
        name = SpiPreconditions.requireClean(name);
        title = SpiPreconditions.cleanNullable(title);
        description = SpiPreconditions.cleanNullable(description);
        mimeType = SpiPreconditions.cleanNullable(mimeType);
        annotations = SpiPreconditions.annotationsOrEmpty(annotations);
        SpiPreconditions.requireMeta(_meta);
    }

}
