package com.amannmalik.mcp.client.elicitation;

import jakarta.json.JsonObject;

/**
 * The client's response to an elicitation request.
 */
public record ElicitationResponse(ElicitationAction action, JsonObject content) {
    public ElicitationResponse {
        if (action == null) throw new IllegalArgumentException("action is required");
    }
}
