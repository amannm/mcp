package com.amannmalik.mcp.schema.core;

import java.util.Optional;

/** Base metadata with a required name and optional title. */
public sealed interface BaseMetadata permits BaseMetadataRecord, Implementation {
    String name();
    Optional<String> title();
}
