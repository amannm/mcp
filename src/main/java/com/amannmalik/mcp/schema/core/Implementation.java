package com.amannmalik.mcp.schema.core;

import java.util.Optional;

/** Name and version of an MCP implementation. */
public record Implementation(String name, Optional<String> title, String version) implements BaseMetadata {
    public Implementation {
        FieldValidations.requireName(name);
        FieldValidations.requireNotBlank("version", version);
        title = title.filter(t -> !t.isBlank());
    }
}
