package com.amannmalik.mcp.resources;

@FunctionalInterface
public interface ResourceListener {
    void updated(ResourceUpdate update);
}
