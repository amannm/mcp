package com.amannmalik.mcp.schema.content;

import com.amannmalik.mcp.schema.core.Annotations;
import com.amannmalik.mcp.schema.core.FieldValidations;
import com.amannmalik.mcp.schema.core.Meta;

import java.util.Optional;

public record ImageContent(
        byte[] data,
        String mimeType,
        Optional<Annotations> annotations,
        Optional<Meta> meta
) implements ContentBlock {
    public ImageContent {
        data = data.clone();
        FieldValidations.requireMimeType(mimeType);
    }

    public String type() { return "image"; }
}
