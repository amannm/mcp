package com.amannmalik.mcp.resources;

public interface ResourceSubscription extends AutoCloseable {
    @Override
    void close();
}
