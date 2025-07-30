package com.amannmalik.mcp.server.resources;

import java.util.List;

public record ReadResourceResult(List<ResourceBlock> contents) {
    public ReadResourceResult {
        contents = contents == null ? List.of() : List.copyOf(contents);
    }
}
