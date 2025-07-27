package com.amannmalik.mcp.schema.content;

import com.amannmalik.mcp.schema.core.Annotations;
import com.amannmalik.mcp.schema.core.FieldValidations;
import com.amannmalik.mcp.schema.core.Meta;

import java.util.Optional;

public record ResourceLink(
        String uri,
        String name,
        Optional<String> title,
        Optional<String> description,
        Optional<String> mimeType,
        Optional<Annotations> annotations,
        Optional<Long> size,
        Optional<Meta> meta
) implements ContentBlock {
    public ResourceLink {
        FieldValidations.requireNotBlank("uri", uri);
        FieldValidations.requireName(name);
        title = title.filter(t -> !t.isBlank());
        description = description.filter(d -> !d.isBlank());
        mimeType = mimeType.filter(m -> !m.isBlank());
        size.ifPresent(s -> { if (s < 0) throw new IllegalArgumentException("size cannot be negative"); });
    }

    public String type() { return "resource_link"; }
}
