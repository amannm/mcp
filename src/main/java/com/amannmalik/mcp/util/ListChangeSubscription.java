package com.amannmalik.mcp.util;

public interface ListChangeSubscription extends AutoCloseable {
    @Override
    void close();
}
