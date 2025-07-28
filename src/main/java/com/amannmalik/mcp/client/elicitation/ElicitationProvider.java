package com.amannmalik.mcp.client.elicitation;

/**
 * Interface implemented by clients to obtain user input when requested by a server.
 */
public interface ElicitationProvider {
    /**
     * Prompt the user according to the request. A timeout of {@code 0} means wait indefinitely.
     */
    ElicitationResponse elicit(ElicitationRequest request, long timeoutMillis) throws InterruptedException;

    default ElicitationResponse elicit(ElicitationRequest request) throws InterruptedException {
        return elicit(request, 0);
    }
}
