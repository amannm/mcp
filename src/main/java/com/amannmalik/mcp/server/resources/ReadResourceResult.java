package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record ReadResourceResult(List<ResourceBlock> contents, JsonObject _meta) {
    public ReadResourceResult {
        contents = contents == null ? List.of() : List.copyOf(contents);
        MetaValidator.requireValid(_meta);
    }
}
