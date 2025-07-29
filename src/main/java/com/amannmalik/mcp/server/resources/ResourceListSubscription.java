package com.amannmalik.mcp.server.resources;

public interface ResourceListSubscription extends AutoCloseable {
    @Override
    void close();
}
