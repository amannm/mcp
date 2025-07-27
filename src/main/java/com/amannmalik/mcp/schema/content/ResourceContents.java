package com.amannmalik.mcp.schema.content;

import com.amannmalik.mcp.schema.core.Meta;

import java.util.Optional;

public interface ResourceContents {
    String uri();
    Optional<String> mimeType();
    Optional<Meta> meta();
}
