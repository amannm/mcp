package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.spi.SamplingAccessPolicy;

public final class DefaultSamplingAccessPolicy implements SamplingAccessPolicy {
    @Override
    public void allow(String principalId) {
    }

    @Override
    public void revoke(String principalId) {
    }

    @Override
    public void requireAllowed(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("principal required");
        }
    }
}
