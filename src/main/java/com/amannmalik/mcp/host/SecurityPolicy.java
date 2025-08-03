package com.amannmalik.mcp.host;

import com.amannmalik.mcp.McpClient;

public interface SecurityPolicy {
    boolean allow(McpClient client);
}
