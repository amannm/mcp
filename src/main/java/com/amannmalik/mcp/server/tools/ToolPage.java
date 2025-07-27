package com.amannmalik.mcp.server.tools;

import java.util.Collections;
import java.util.List;

/** Result of a tool listing request. */
public record ToolPage(List<Tool> tools, String nextCursor) {
    public ToolPage {
        tools = tools == null || tools.isEmpty() ? List.of() : List.copyOf(tools);
    }

    public List<Tool> tools() {
        return Collections.unmodifiableList(tools);
    }
}
