package com.amannmalik.mcp.server.tools;

import java.util.List;
import jakarta.json.JsonObject;
import com.amannmalik.mcp.validation.MetaValidator;

public record ListToolsResult(List<Tool> tools,
                              String nextCursor,
                              JsonObject _meta) {
    public ListToolsResult {
        tools = tools == null || tools.isEmpty() ? List.of() : List.copyOf(tools);
        MetaValidator.requireValid(_meta);
    }
}
