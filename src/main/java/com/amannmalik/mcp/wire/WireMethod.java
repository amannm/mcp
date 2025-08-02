package com.amannmalik.mcp.wire;

/** Common interface for request and notification method enums. */
public sealed interface WireMethod permits RequestMethod, NotificationMethod {
    String method();
}
