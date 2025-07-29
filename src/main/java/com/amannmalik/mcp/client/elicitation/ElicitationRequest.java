package com.amannmalik.mcp.client.elicitation;

import com.amannmalik.mcp.validation.ElicitationSchemaValidator;
import com.amannmalik.mcp.validation.InputSanitizer;
import jakarta.json.JsonObject;

public record ElicitationRequest(String message, JsonObject requestedSchema) {
    public ElicitationRequest(String message, JsonObject requestedSchema) {
        if (message == null || requestedSchema == null) {
            throw new IllegalArgumentException("message and requestedSchema are required");
        }
        this.message = InputSanitizer.requireClean(message);
        ElicitationSchemaValidator.requireValid(requestedSchema);
        this.requestedSchema = requestedSchema;
    }
}
