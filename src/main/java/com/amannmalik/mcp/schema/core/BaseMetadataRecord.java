package com.amannmalik.mcp.schema.core;

import java.util.Optional;

/** Simple implementation of {@link BaseMetadata}. */
public record BaseMetadataRecord(String name, Optional<String> title) implements BaseMetadata {
    public BaseMetadataRecord {
        FieldValidations.requireName(name);
        title = title.filter(t -> !t.isBlank());
    }
}
