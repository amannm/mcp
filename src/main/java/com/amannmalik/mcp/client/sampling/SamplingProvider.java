package com.amannmalik.mcp.client.sampling;

import java.io.IOException;

/** Interface for integrating language model providers. */
public interface SamplingProvider extends AutoCloseable {
    CreateMessageResponse createMessage(CreateMessageRequest request) throws IOException;

    @Override
    default void close() throws IOException {}
}
