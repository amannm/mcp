package com.amannmalik.mcp.schema.core;

import java.util.Optional;

/** Base metadata with a required name and optional title. */
public interface BaseMetadata {
    String name();
    Optional<String> title();
}
