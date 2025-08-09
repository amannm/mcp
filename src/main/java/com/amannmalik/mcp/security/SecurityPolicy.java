package com.amannmalik.mcp.security;

import com.amannmalik.mcp.core.McpClient;

public interface SecurityPolicy {
    boolean allow(McpClient client);
}
