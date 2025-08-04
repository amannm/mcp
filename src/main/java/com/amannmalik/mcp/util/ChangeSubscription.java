package com.amannmalik.mcp.util;

public interface ChangeSubscription extends AutoCloseable {
    @Override
    void close();
}
