package com.amannmalik.mcp.api;

public record ServerFeatures(
        boolean resourcesSubscribe,
        boolean resourcesListChanged,
        boolean toolsListChanged,
        boolean promptsListChanged
) {
}
