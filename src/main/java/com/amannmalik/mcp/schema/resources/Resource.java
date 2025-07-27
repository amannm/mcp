package com.amannmalik.mcp.schema.resources;

import com.amannmalik.mcp.schema.core.*;

import java.util.Optional;

/** Known resource available from the server. */
public record Resource(
        String name,
        Optional<String> title,
        String uri,
        Optional<String> description,
        Optional<String> mimeType,
        Optional<Annotations> annotations,
        Optional<Long> size,
        Optional<Meta> _meta
) implements BaseMetadata {
    public Resource {
        FieldValidations.requireName(name);
        uri = FieldValidations.requireUri(uri);
        title = title.filter(t -> !t.isBlank());
        description = description.filter(d -> !d.isBlank());
        mimeType = mimeType.filter(m -> !m.isBlank());
        size = size.map(s -> FieldValidations.requireNonNegative("size", s));
    }
}
