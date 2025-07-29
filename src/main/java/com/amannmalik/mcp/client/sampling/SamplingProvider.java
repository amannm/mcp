package com.amannmalik.mcp.client.sampling;


public interface SamplingProvider extends AutoCloseable {
    CreateMessageResponse createMessage(CreateMessageRequest request);

    @Override
    default void close() {}
}
