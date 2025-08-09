package com.amannmalik.mcp.core;

public record ServerFeatures(
        boolean resourcesSubscribe,
        boolean resourcesListChanged,
        boolean toolsListChanged,
        boolean promptsListChanged
) {
}
