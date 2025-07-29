package com.amannmalik.mcp.security;

import com.amannmalik.mcp.client.McpClient;

public interface SecurityPolicy {
    boolean allow(McpClient client);
}
