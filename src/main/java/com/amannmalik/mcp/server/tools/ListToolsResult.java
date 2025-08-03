package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.util.Immutable;
import com.amannmalik.mcp.server.roots.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record ListToolsResult(List<Tool> tools,
                              String nextCursor,
                              JsonObject _meta) {
    public ListToolsResult {
        tools = Immutable.list(tools);
        MetaValidator.requireValid(_meta);
    }
}
