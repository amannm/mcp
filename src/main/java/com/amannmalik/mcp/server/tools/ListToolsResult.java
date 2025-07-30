package com.amannmalik.mcp.server.tools;

import java.util.Collections;
import java.util.List;

/**
 * Result for a {@code tools/list} request.
 */
public record ListToolsResult(List<Tool> tools, String nextCursor) {
    public ListToolsResult {
        tools = tools == null || tools.isEmpty() ? List.of() : List.copyOf(tools);
    }

    public List<Tool> tools() {
        return Collections.unmodifiableList(tools);
    }
}
