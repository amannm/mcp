package com.amannmalik.mcp.spi;

public interface ResourceAccessPolicy {
    void allow(String principalId, Role audience);

    void revoke(String principalId, Role audience);

    void requireAllowed(Principal principal, Annotations annotations);
}
