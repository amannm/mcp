package com.amannmalik.mcp.schema.resources;

import com.amannmalik.mcp.schema.core.*;

import java.util.Optional;

/** Template description for resources available from the server. */
public record ResourceTemplate(
        String name,
        Optional<String> title,
        String uriTemplate,
        Optional<String> description,
        Optional<String> mimeType,
        Optional<Annotations> annotations,
        Optional<Meta> _meta
) implements BaseMetadata {
    public ResourceTemplate {
        FieldValidations.requireName(name);
        uriTemplate = FieldValidations.requireNotBlank("uriTemplate", uriTemplate);
        title = title.filter(t -> !t.isBlank());
        description = description.filter(d -> !d.isBlank());
        mimeType = mimeType.filter(m -> !m.isBlank());
    }
}
