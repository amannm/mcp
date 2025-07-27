package com.amannmalik.mcp.server.resources;

@FunctionalInterface
public interface ResourceListener {
    void updated(ResourceUpdate update);
}
