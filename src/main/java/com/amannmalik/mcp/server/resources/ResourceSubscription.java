package com.amannmalik.mcp.server.resources;

public interface ResourceSubscription extends AutoCloseable {
    @Override
    void close();
}
