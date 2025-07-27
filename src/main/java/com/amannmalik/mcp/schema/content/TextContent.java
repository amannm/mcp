package com.amannmalik.mcp.schema.content;

import com.amannmalik.mcp.schema.core.Annotations;
import com.amannmalik.mcp.schema.core.FieldValidations;
import com.amannmalik.mcp.schema.core.Meta;

import java.util.Optional;

public record TextContent(
        String text,
        Optional<Annotations> annotations,
        Optional<Meta> meta
) implements ContentBlock {
    public TextContent {
        FieldValidations.requireNotBlank("text", text);
    }

    public String type() { return "text"; }
}
