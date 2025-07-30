package com.amannmalik.mcp.server.tools;

import java.util.List;

public record ListToolsResult(List<Tool> tools, String nextCursor) {
    public ListToolsResult {
        tools = tools == null || tools.isEmpty() ? List.of() : List.copyOf(tools);
    }
}
