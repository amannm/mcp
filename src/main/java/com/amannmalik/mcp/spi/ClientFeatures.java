package com.amannmalik.mcp.spi;

public record ClientFeatures(
        boolean rootsListChanged
) {
    public static final ClientFeatures EMPTY = new ClientFeatures(false);
}
