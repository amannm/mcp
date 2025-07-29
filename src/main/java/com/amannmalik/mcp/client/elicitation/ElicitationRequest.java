package com.amannmalik.mcp.client.elicitation;

import jakarta.json.JsonObject;


public record ElicitationRequest(String message, JsonObject requestedSchema) {
    public ElicitationRequest {
        if (message == null || requestedSchema == null) {
            throw new IllegalArgumentException("message and requestedSchema are required");
        }
    }
}
