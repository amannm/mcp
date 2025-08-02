package com.amannmalik.mcp.server.tools;

import com.amannmalik.mcp.validation.MetaValidator;
import jakarta.json.JsonObject;

import java.util.List;

public record ListToolsResult(List<Tool> tools,
                              String nextCursor,
                              JsonObject _meta) {
    public ListToolsResult {
        tools = tools == null || tools.isEmpty() ? List.of() : List.copyOf(tools);
        MetaValidator.requireValid(_meta);
    }
}
