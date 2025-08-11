package com.amannmalik.mcp.spi;

public interface ChangeSubscription extends AutoCloseable {
    @Override
    void close();
}
