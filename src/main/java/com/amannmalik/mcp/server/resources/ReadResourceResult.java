package com.amannmalik.mcp.server.resources;

import java.util.List;
import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ReadResourceResult(List<ResourceBlock> contents, JsonObject _meta) {
    public ReadResourceResult {
        contents = contents == null ? List.of() : List.copyOf(contents);
        MetaValidator.requireValid(_meta);
    }
}
