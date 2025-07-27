package com.amannmalik.mcp.schema.content;

import com.amannmalik.mcp.schema.core.Annotations;
import com.amannmalik.mcp.schema.core.Meta;

import java.util.Optional;

public record EmbeddedResource(
        ResourceContents resource,
        Optional<Annotations> annotations,
        Optional<Meta> meta
) implements ContentBlock {
    public EmbeddedResource {
    }

    public String type() { return "resource"; }
}
