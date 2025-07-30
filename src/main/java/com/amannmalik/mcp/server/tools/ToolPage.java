package com.amannmalik.mcp.server.tools;

import java.util.List;

public record ToolPage(List<Tool> tools, String nextCursor) {
    public ToolPage {
        tools = tools == null || tools.isEmpty() ? List.of() : List.copyOf(tools);
    }
}
