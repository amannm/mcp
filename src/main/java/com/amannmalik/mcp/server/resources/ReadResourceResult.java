package com.amannmalik.mcp.server.resources;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record ReadResourceResult(List<ResourceBlock> contents, JsonObject _meta) {
    public ReadResourceResult {
        contents = Immutable.list(contents);
        MetaValidator.requireValid(_meta);
    }
}
