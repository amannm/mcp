package com.amannmalik.mcp.util;

import com.amannmalik.mcp.config.McpConfiguration;

public final class Timeouts {
    private Timeouts() {
    }

    public static final long DEFAULT_TIMEOUT_MS =
            McpConfiguration.current().system().timeouts().defaultMs();
}
