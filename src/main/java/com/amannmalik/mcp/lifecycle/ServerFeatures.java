package com.amannmalik.mcp.lifecycle;

public record ServerFeatures(
        boolean resourcesSubscribe,
        boolean resourcesListChanged,
        boolean toolsListChanged,
        boolean promptsListChanged
) {
}
