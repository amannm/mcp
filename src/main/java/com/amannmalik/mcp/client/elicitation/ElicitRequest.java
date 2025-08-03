package com.amannmalik.mcp.client.elicitation;

import com.amannmalik.mcp.validation.*;
import jakarta.json.JsonObject;

public record ElicitRequest(String message,
                            JsonObject requestedSchema,
                            JsonObject _meta) {
    public ElicitRequest(String message, JsonObject requestedSchema, JsonObject _meta) {
        if (message == null || requestedSchema == null) {
            throw new IllegalArgumentException("message and requestedSchema are required");
        }
        this.message = InputSanitizer.requireClean(message);
        ElicitationSchemaValidator.requireValid(requestedSchema);
        this.requestedSchema = requestedSchema;
        MetaValidator.requireValid(_meta);
        this._meta = _meta;
    }
}
