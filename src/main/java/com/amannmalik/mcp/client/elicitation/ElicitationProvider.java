package com.amannmalik.mcp.client.elicitation;


public interface ElicitationProvider {
    
    ElicitationResponse elicit(ElicitationRequest request, long timeoutMillis) throws InterruptedException;

    default ElicitationResponse elicit(ElicitationRequest request) throws InterruptedException {
        return elicit(request, 0);
    }
}
