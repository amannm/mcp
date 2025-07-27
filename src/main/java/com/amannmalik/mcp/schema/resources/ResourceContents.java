package com.amannmalik.mcp.schema.resources;

import com.amannmalik.mcp.schema.core.Meta;

import java.util.Optional;

/** Contents of a resource or sub-resource. */
public sealed interface ResourceContents permits TextResourceContents, BlobResourceContents {
    String uri();
    Optional<String> mimeType();
    Optional<Meta> _meta();
}
