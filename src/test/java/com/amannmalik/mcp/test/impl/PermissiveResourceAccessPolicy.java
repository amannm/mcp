package com.amannmalik.mcp.test.impl;

import com.amannmalik.mcp.spi.*;

public final class PermissiveResourceAccessPolicy implements ResourceAccessPolicy {
    @Override
    public void requireAllowed(Principal principal, Annotations annotations) {
    }
}
