package com.amannmalik.mcp.security;

import com.amannmalik.mcp.McpClient;

public interface SecurityPolicy {
    boolean allow(McpClient client);
}
