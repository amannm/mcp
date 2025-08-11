package com.amannmalik.mcp.api.model;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

import java.util.List;

public record ListResourceTemplatesResult(List<ResourceTemplate> resourceTemplates,
                                          String nextCursor,
                                          JsonObject _meta) {
    public ListResourceTemplatesResult {
        resourceTemplates = Immutable.list(resourceTemplates);
        ValidationUtil.requireMeta(_meta);
    }
}
