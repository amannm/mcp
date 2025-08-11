package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.codec.CreateMessageRequestJsonCodec;
import jakarta.json.JsonObject;

/// - [Sampling](specification/2025-06-18/client/sampling.mdx)
/// - [MCP sampling specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:135-150)
public non-sealed interface SamplingProvider extends ExecutingProvider<SamplingMessage, CreateMessageResponse> {
    CreateMessageResponse createMessage(CreateMessageRequest request, long timeoutMillis) throws InterruptedException;

    default CreateMessageResponse createMessage(CreateMessageRequest request) throws InterruptedException {
        return createMessage(request, 0);
    }

    @Override
    default CreateMessageResponse execute(String name, JsonObject args) throws InterruptedException {
        return createMessage(new CreateMessageRequestJsonCodec().fromJson(args), 0);
    }
}
