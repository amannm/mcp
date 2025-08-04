package com.amannmalik.mcp.lifecycle;

import com.amannmalik.mcp.config.McpConfiguration;

/// - [Lifecycle](specification/2025-06-18/basic/lifecycle.mdx)
public final class Protocol {
    private Protocol() {
    }

    public static final String LATEST_VERSION =
            McpConfiguration.current().system().version();

    public static final String PREVIOUS_VERSION =
            McpConfiguration.current().system().compatibilityVersion();
}

