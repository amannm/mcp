package com.amannmalik.mcp.resources;

import com.amannmalik.mcp.api.model.ResourceBlock;
import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ReadResourceResult(List<ResourceBlock> contents, JsonObject _meta) {

    public ReadResourceResult {
        contents = Immutable.list(contents);
        ValidationUtil.requireMeta(_meta);
    }

}
