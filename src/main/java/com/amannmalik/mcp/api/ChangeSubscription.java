package com.amannmalik.mcp.api;

public interface ChangeSubscription extends AutoCloseable {
    @Override
    void close();
}
