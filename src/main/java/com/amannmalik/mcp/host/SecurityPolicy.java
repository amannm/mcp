package com.amannmalik.mcp.host;

import com.amannmalik.mcp.client.McpClient;

/** Determines whether a client is allowed to connect. */
public interface SecurityPolicy {
    boolean allow(McpClient client);
}
