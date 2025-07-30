package com.amannmalik.mcp.lifecycle;

/**
 * Optional feature flags for client capabilities.
 */
public record ClientFeatures(
        boolean rootsListChanged
) {
    public static final ClientFeatures EMPTY = new ClientFeatures(false);
}
