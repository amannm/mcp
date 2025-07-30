package com.amannmalik.mcp.server.tools;

import java.util.Collections;
import java.util.List;

public record ListToolsResult(List<Tool> tools, String nextCursor) {
    public ListToolsResult {
        tools = tools == null || tools.isEmpty() ? List.of() : List.copyOf(tools);
    }

    public List<Tool> tools() {
        return Collections.unmodifiableList(tools);
    }
}
