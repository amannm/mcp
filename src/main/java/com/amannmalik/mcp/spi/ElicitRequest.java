package com.amannmalik.mcp.spi;

import com.amannmalik.mcp.core.ElicitRequestContract;
import jakarta.json.JsonObject;

public record ElicitRequest(String message, JsonObject requestedSchema, JsonObject _meta) {
    public ElicitRequest {
        message = ElicitRequestContract.sanitizeMessage(message);
        requestedSchema = ElicitRequestContract.sanitizeSchema(requestedSchema);
        _meta = ElicitRequestContract.requireMeta(_meta);
    }
}
