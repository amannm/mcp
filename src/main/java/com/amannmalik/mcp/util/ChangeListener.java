package com.amannmalik.mcp.util;

@FunctionalInterface
public interface ChangeListener<T> {
    void changed(T change);
}

