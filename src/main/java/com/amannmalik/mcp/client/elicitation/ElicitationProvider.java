package com.amannmalik.mcp.client.elicitation;

public interface ElicitationProvider {

    ElicitResult elicit(ElicitRequest request, long timeoutMillis) throws InterruptedException;

    default ElicitResult elicit(ElicitRequest request) throws InterruptedException {
        return elicit(request, 0);
    }
}
