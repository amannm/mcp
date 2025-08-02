package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.config.McpConfiguration;

public final class Protocol {
    private Protocol() {
    }

    public static final String LATEST_VERSION =
            McpConfiguration.current().system().protocol().version();

    public static final String PREVIOUS_VERSION =
            McpConfiguration.current().system().protocol().compatibilityVersion();
}

