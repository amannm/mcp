package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.util.ValidationUtil;
import jakarta.json.JsonObject;

public record ElicitRequest(String message, JsonObject requestedSchema, JsonObject _meta) {
    public ElicitRequest(String message, JsonObject requestedSchema, JsonObject _meta) {
        if (message == null || requestedSchema == null) {
            throw new IllegalArgumentException("message and requestedSchema are required");
        }
        this.message = ValidationUtil.requireClean(message);
        ValidationUtil.requireElicitSchema(requestedSchema);
        this.requestedSchema = requestedSchema;
        ValidationUtil.requireMeta(_meta);
        this._meta = _meta;
    }
}
