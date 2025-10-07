package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.Annotations;
import com.amannmalik.mcp.spi.Principal;
import com.amannmalik.mcp.spi.ResourceAccessPolicy;
import com.amannmalik.mcp.spi.Role;

public final class DefaultResourceAccessPolicy implements ResourceAccessPolicy {
    @Override
    public void allow(String principalId, Role audience) {
    }

    @Override
    public void revoke(String principalId, Role audience) {
    }

    @Override
    public void requireAllowed(Principal principal, Annotations annotations) {
        if (principal == null) {
            throw new IllegalArgumentException("principal required");
        }
        if (annotations == null) {
            return;
        }
    }
}
