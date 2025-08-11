package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.api.Pagination;
import com.amannmalik.mcp.api.model.ElicitRequest;
import com.amannmalik.mcp.api.model.ElicitResult;
import com.amannmalik.mcp.codec.ElicitRequestJsonCodec;
import com.amannmalik.mcp.util.ExecutingProvider;
import jakarta.json.JsonObject;

import java.util.List;

/// - [Elicitation](specification/2025-06-18/client/elicitation.mdx)
/// - [MCP elicitation specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:116-131)
public non-sealed interface ElicitationProvider extends ExecutingProvider<ElicitRequest, ElicitResult> {
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
