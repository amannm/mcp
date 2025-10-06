package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;

public final class PermissiveSamplingAccessPolicy implements SamplingAccessPolicy {
    @Override
    public void requireAllowed(Principal principal) {
    }
}
