package com.amannmalik.mcp.spi;

public interface ToolAccessPolicy {
    void allow(String principalId, String tool);

    void revoke(String principalId, String tool);

    void requireAllowed(Principal principal, Tool tool);
}
