package com.amannmalik.mcp.core;

public record ClientFeatures(
        boolean rootsListChanged
) {
    public static final ClientFeatures EMPTY = new ClientFeatures(false);
}
