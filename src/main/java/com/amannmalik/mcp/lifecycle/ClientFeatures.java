package com.amannmalik.mcp.lifecycle;

public record ClientFeatures(
        boolean rootsListChanged
) {
    public static final ClientFeatures EMPTY = new ClientFeatures(false);
}
