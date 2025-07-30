package com.amannmalik.mcp.lifecycle;

/**
 * Optional feature flags for server capabilities.
 */
public record ServerFeatures(
        boolean resourcesSubscribe,
        boolean resourcesListChanged,
        boolean toolsListChanged,
        boolean promptsListChanged
) {
}
