package com.amannmalik.mcp.api;

import com.amannmalik.mcp.codec.ElicitRequestJsonCodec;
import jakarta.json.JsonObject;

import java.util.List;

/// - [Elicitation](specification/2025-06-18/client/elicitation.mdx)
/// - [MCP elicitation specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:116-131)
public interface ElicitationProvider extends ExecutingProvider<ElicitRequest, ElicitResult> {
    ElicitResult elicit(ElicitRequest request, long timeoutMillis) throws InterruptedException;

    default ElicitResult elicit(ElicitRequest request) throws InterruptedException {
        return elicit(request, 0);
    }

    @Override
    default Pagination.Page<ElicitRequest> list(String cursor) {
        return new Pagination.Page<>(List.of(), null);
    }

    @Override
    default ElicitResult execute(String name, JsonObject args) throws InterruptedException {
        return elicit(new ElicitRequestJsonCodec().fromJson(args), 0);
    }
}
