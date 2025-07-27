package com.amannmalik.mcp.schema.resources;

import com.amannmalik.mcp.schema.core.FieldValidations;
import com.amannmalik.mcp.schema.core.Meta;

import java.util.Optional;

/** Text-based resource contents. */
public record TextResourceContents(
        String uri,
        String text,
        Optional<String> mimeType,
        Optional<Meta> _meta
) implements ResourceContents {
    public TextResourceContents {
        uri = FieldValidations.requireUri(uri);
        text = FieldValidations.requireNotBlank("text", text);
        mimeType = mimeType.filter(m -> !m.isBlank());
    }
}
