package com.amannmalik.mcp.server.tools;

public interface ToolListSubscription extends AutoCloseable {
    @Override
    void close();
}
