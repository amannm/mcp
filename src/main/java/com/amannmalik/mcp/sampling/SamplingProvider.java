package com.amannmalik.mcp.sampling;

import com.amannmalik.mcp.core.ExecutingProvider;
import com.amannmalik.mcp.util.Pagination;
import jakarta.json.JsonObject;

import java.util.List;

/// - [Sampling](specification/2025-06-18/client/sampling.mdx)
/// - [MCP sampling specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:135-150)
public interface SamplingProvider extends ExecutingProvider<SamplingMessage, CreateMessageResponse> {
    CreateMessageResponse createMessage(CreateMessageRequest request, long timeoutMillis) throws InterruptedException;

    default CreateMessageResponse createMessage(CreateMessageRequest request) throws InterruptedException {
        return createMessage(request, 0);
    }

    @Override
    default Pagination.Page<SamplingMessage> list(String cursor) {
        return new Pagination.Page<>(List.of(), null);
    }

    @Override
    default CreateMessageResponse execute(String name, JsonObject args) throws InterruptedException {
        return createMessage(CreateMessageRequest.CODEC.fromJson(args), 0);
    }
}
