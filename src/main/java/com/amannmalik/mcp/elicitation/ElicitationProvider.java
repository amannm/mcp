package com.amannmalik.mcp.elicitation;

/// - [Elicitation](specification/2025-06-18/client/elicitation.mdx)
/// - [MCP elicitation specification conformance](src/test/resources/com/amannmalik/mcp/mcp_conformance.feature:116-131)
public interface ElicitationProvider {

    ElicitResult elicit(ElicitRequest request, long timeoutMillis) throws InterruptedException;

    default ElicitResult elicit(ElicitRequest request) throws InterruptedException {
        return elicit(request, 0);
    }
}
