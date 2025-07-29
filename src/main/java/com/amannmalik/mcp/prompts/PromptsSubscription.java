package com.amannmalik.mcp.prompts;

public interface PromptsSubscription extends AutoCloseable {
    @Override
    void close();
}
