package com.amannmalik.mcp.sampling;

/// - [Sampling](specification/2025-06-18/client/sampling.mdx)
/// - [MCP sampling specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:135-150)
public interface SamplingProvider extends AutoCloseable {
    CreateMessageResponse createMessage(CreateMessageRequest request, long timeoutMillis) throws InterruptedException;

    default CreateMessageResponse createMessage(CreateMessageRequest request) throws InterruptedException {
        return createMessage(request, 0);
    }

    @Override
    default void close() {
    }
}
