package com.amannmalik.mcp.security;

import com.amannmalik.mcp.transport.McpClient;

public interface SecurityPolicy {
    boolean allow(McpClient client);
}
