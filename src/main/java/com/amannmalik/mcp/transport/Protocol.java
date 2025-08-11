package com.amannmalik.mcp.transport;

import com.amannmalik.mcp.api.McpServerConfiguration;

/// - [Lifecycle](specification/2025-06-18/basic/lifecycle.mdx)
public final class Protocol {
    private Protocol() {
    }    public static final String LATEST_VERSION =
            McpServerConfiguration.defaultConfiguration().version();
    public static final String PREVIOUS_VERSION =
            McpServerConfiguration.defaultConfiguration().compatibilityVersion();


}

