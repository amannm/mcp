package com.amannmalik.mcp.api.model;

public record ServerFeatures(
        boolean resourcesSubscribe,
        boolean resourcesListChanged,
        boolean toolsListChanged,
        boolean promptsListChanged
) {
}
