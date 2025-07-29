package com.amannmalik.mcp.host;

import com.amannmalik.mcp.client.McpClient;


public interface SecurityPolicy {
    boolean allow(McpClient client);
}
