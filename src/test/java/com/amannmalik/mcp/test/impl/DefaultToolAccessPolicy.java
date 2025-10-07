package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.spi.Tool;
import com.amannmalik.mcp.spi.ToolAccessPolicy;

public final class DefaultToolAccessPolicy implements ToolAccessPolicy {
    @Override
    public void allow(String principalId, String tool) {
    }

    @Override
    public void revoke(String principalId, String tool) {
    }

    @Override
    public void requireAllowed(Principal principal, Tool tool) {
        if (principal == null) {
            throw new IllegalArgumentException("principal required");
        }
        if (tool == null) {
            throw new IllegalArgumentException("tool required");
        }
    }
}
