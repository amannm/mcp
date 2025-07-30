package com.amannmalik.mcp.client.sampling;

public interface SamplingProvider extends AutoCloseable {
    CreateMessageResponse createMessage(CreateMessageRequest request, long timeoutMillis) throws InterruptedException;

    default CreateMessageResponse createMessage(CreateMessageRequest request) throws InterruptedException {
        return createMessage(request, 0);
    }

    @Override
    default void close() {
    }
}
