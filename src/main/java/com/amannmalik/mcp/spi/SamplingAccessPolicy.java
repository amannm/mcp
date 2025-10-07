package com.amannmalik.mcp.spi;

public interface SamplingAccessPolicy {
    void allow(String principalId);

    void revoke(String principalId);

    void requireAllowed(Principal principal);
}

