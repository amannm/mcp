package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;

public final class PermissiveToolAccessPolicy implements ToolAccessPolicy {
    @Override
    public void requireAllowed(Principal principal, Tool tool) {
    }
}
