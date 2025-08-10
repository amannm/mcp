package com.amannmalik.mcp.api;

@FunctionalInterface
public interface ResourceAccessPolicy {
    void requireAllowed(Principal principal, Annotations annotations);
}
